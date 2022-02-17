/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMethodMappingNamingStrategy;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Abstract base class for {@link HandlerMapping} implementations that define
 * a mapping between a request and a {@link HandlerMethod}.
 *
 * <p>For each registered handler method, a unique mapping is maintained with
 * subclasses defining the details of the mapping type {@code <T>}.
 *
 * 实现了initializingBean接口，其实主要的注册操作则是通过afterPropertiesSet()接口方法来调用的
 * 它是带有泛型T的。（包含HandlerMethod与传入请求匹配所需条件的handlerMethod的映射）
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1  Spring3.1之后才出现
 * @param <T> the mapping for a {@link HandlerMethod} containing the conditions
 * needed to match the handler method to an incoming request.
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * Bean name prefix for target beans behind scoped proxies. Used to exclude those
	 * targets from handler method detection, in favor of the corresponding proxies.
	 * <p>We're not checking the autowire-candidate status here, which is how the
	 * proxy target filtering problem is being handled at the autowiring level,
	 * since autowire-candidate may have been turned to {@code false} for other
	 * reasons, while still expecting the bean to be eligible for handler methods.
	 * <p>Originally defined in {@link org.springframework.aop.scope.ScopedProxyUtils}
	 * but duplicated here to avoid a hard dependency on the spring-aop module.
	 *
	 * SCOPED_TARGET的BeanName的前缀
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

	private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
			new HandlerMethod(new EmptyHandler(), ClassUtils.getMethod(EmptyHandler.class, "handle"));

	/**
	 * 跨域相关
	 */
	private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();

	static {
		ALLOW_CORS_CONFIG.addAllowedOriginPattern("*");
		ALLOW_CORS_CONFIG.addAllowedMethod("*");
		ALLOW_CORS_CONFIG.addAllowedHeader("*");
		ALLOW_CORS_CONFIG.setAllowCredentials(true);
	}

	/**
	 * 默认不会去祖先容器里面找Handlers
	 */
	private boolean detectHandlerMethodsInAncestorContexts = false;

	/**
		@since 4.1提供的新接口
			为HandlerMethod的映射分配名称的策略接口，只有一个方法getName()
	        唯一实现为：RequestMappingInfoHandlerMethodMappingNamingStrategy
	        该策略为：@RequestMapping指定了name属性，那就以指定的为准，否则策略为：取出Controller所有的`大写字母` + # + method.getName()
	            如：AppoloController#match方法  最终的name为：AC#match
	        当然这个你也可以自己实现这个接口，然后set进来即可（只是一般没必要这么做）
	 */
	@Nullable
	private HandlerMethodMappingNamingStrategy<T> namingStrategy;


	/**
	 * 内部类：负责注册
	 */
	private final MappingRegistry mappingRegistry = new MappingRegistry();


	@Override
	public void setPatternParser(PathPatternParser patternParser) {
		Assert.state(this.mappingRegistry.getRegistrations().isEmpty(),
				"PathPatternParser must be set before the initialization of " +
						"request mappings through InitializingBean#afterPropertiesSet.");
		super.setPatternParser(patternParser);
	}

	/**
	 * Whether to detect handler methods in beans in ancestor ApplicationContexts.
	 * <p>Default is "false": Only beans in the current ApplicationContext are
	 * considered, i.e. only in the context that this HandlerMapping itself
	 * is defined in (typically the current DispatcherServlet's context).
	 * <p>Switch this flag on to detect handler beans in ancestor contexts
	 * (typically the Spring root WebApplicationContext) as well.
	 * @see #getCandidateBeanNames()
	 */
	public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
		this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
	}

	/**
	 * Configure the naming strategy to use for assigning a default name to every
	 * mapped handler method.
	 * <p>The default naming strategy is based on the capital letters of the
	 * class name followed by "#" and then the method name, e.g. "TC#getFoo"
	 * for a class named TestController with method getFoo.
	 */
	public void setHandlerMethodMappingNamingStrategy(HandlerMethodMappingNamingStrategy<T> namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	/**
	 * Return the configured naming strategy or {@code null}.
	 */
	@Nullable
	public HandlerMethodMappingNamingStrategy<T> getNamingStrategy() {
		return this.namingStrategy;
	}

	/**
	 * Return a (read-only) map with all mappings and HandlerMethod's.
	 * 使用的是读写锁  比如此处使用的是读锁   获得所有的注册进去的Handler的Map
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		this.mappingRegistry.acquireReadLock();
		try {
			return Collections.unmodifiableMap(
					this.mappingRegistry.getRegistrations().entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().handlerMethod)));
		}
		finally {
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * Return the handler methods for the given mapping name.
	 * 根据mappingName来获取一个Handler
	 *
	 *
	 * @param mappingName the mapping name
	 * @return a list of matching HandlerMethod's or {@code null}; the returned
	 * list will never be modified and is safe to iterate.
	 * @see #setHandlerMethodMappingNamingStrategy
	 */
	@Nullable
	public List<HandlerMethod> getHandlerMethodsForMappingName(String mappingName) {
		return this.mappingRegistry.getHandlerMethodsByMappingName(mappingName);
	}

	/**
	 * Return the internal mapping registry. Provided for testing purposes.
	 */
	MappingRegistry getMappingRegistry() {
		return this.mappingRegistry;
	}

	/**
	 * Register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 *
	 * 最终都是委托给mappingRegistry去做了注册的工作   此处日志级别为trace级别
	 *
	 * @param mapping the mapping for the handler method
	 * @param handler the handler
	 * @param method the method
	 */
	public void registerMapping(T mapping, Object handler, Method method) {
		if (logger.isTraceEnabled()) {
			logger.trace("Register \"" + mapping + "\" to " + method.toGenericString());
		}
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * Un-register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 * @param mapping the mapping to unregister
	 */
	public void unregisterMapping(T mapping) {
		if (logger.isTraceEnabled()) {
			logger.trace("Unregister mapping \"" + mapping + "\"");
		}
		this.mappingRegistry.unregister(mapping);
	}


	// Handler method detection

	/**
	 * Detects handler methods at initialization.
	 * 初始化HandlerMethods的入口
	 * @see #initHandlerMethods
	 */
	@Override
	public void afterPropertiesSet() {
		// 初始化Handler方法, 就是对编写的Control层方法进行注册, key为方法的访问路径, value为对方法的包装类HandlerMethod
		initHandlerMethods();
	}

	/**
	 * Scan beans in the ApplicationContext, detect and register handler methods.
	 *
	 * 观察是如何实现加载HandlerMethod
	 * @see #getCandidateBeanNames()
	 * @see #processCandidateBean
	 * @see #handlerMethodsInitialized
	 */
	protected void initHandlerMethods() {
		// 遍历所有bean
		// getCandidateBeanNames：Object.class相当于拿到当前容器（一般都是当前容器） 内所有的BeanDefinition信息
		// 如果阁下容器隔离到到的话，这里一般只会拿到@Controller标注的web组件  以及其它相关web组件的  不会非常的多的~~~~
		for (String beanName : getCandidateBeanNames()) {
			// 排除scopedTarget.开头的Bean（排除Scoped目标类型Bean）
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				// 查找是handler的bean（会在每个Bean里面找处理方法，HandlerMethod，然后注册进去）
				processCandidateBean(beanName);
			}
		}
		// 输出一句日志：debug日志或者trace日志
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
	 * Determine the names of candidate beans in the application context.
	 * 获取所有Object类型的bean
	 * @since 5.1
	 * @see #setDetectHandlerMethodsInAncestorContexts
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors
	 */
	protected String[] getCandidateBeanNames() {
		return (this.detectHandlerMethodsInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
				obtainApplicationContext().getBeanNamesForType(Object.class));
	}

	/**
	 * Determine the type of the specified candidate bean and call
	 * {@link #detectHandlerMethods} if identified as a handler type.
	 * <p>This implementation avoids bean creation through checking
	 * {@link org.springframework.beans.factory.BeanFactory#getType}
	 * and calling {@link #detectHandlerMethods} with the bean name.
	 *
	 * 确定指定的候选bean的类型，如果标识为Handler类型，则调用DetectHandlerMethods
	 *   isHandler(beanType):判断这个type是否为Handler类型   它是个抽象方法，由子类去决定到底啥才叫Handler
	 *   `RequestMappingHandlerMapping`的判断依据为：该类上标注了@Controller注解或者@Controller注解  就算作是一个Handler
	 *    所以此处：@Controller起到了一个特殊的作用，不能等价于@Component的哟
	 *
	 *
	 * @param beanName the name of the candidate bean
	 * @since 5.1
	 * @see #isHandler
	 * @see #detectHandlerMethods
	 */
	protected void processCandidateBean(String beanName) {
		Class<?> beanType = null;
		try {
			// 获取对应bean的Class
			beanType = obtainApplicationContext().getType(beanName);
		}
		catch (Throwable ex) {
			// An unresolvable bean type, probably from a lazy bean - let's ignore it.
			if (logger.isTraceEnabled()) {
				logger.trace("Could not resolve type for bean '" + beanName + "'", ex);
			}
		}

		// 如果获取的BeanName类型不为空，且是一个处理器类型的Bean
		// 如果beanType上有Controller注解或者RequestMapping注解则是handler
		if (beanType != null && isHandler(beanType)) {
			// 对Controller类中所有的方法进行探测
			detectHandlerMethods(beanName);
		}
	}

	/**
	 * Look for handler methods in the specified handler bean.
	 * 在指定的Handler的bean中查找处理程序方法Methods  找到就注册进去：mappingRegistry
	 *
	 * @param handler either a bean name or an actual handler instance
	 * @see #getMappingForMethod
	 */
	protected void detectHandlerMethods(Object handler) {
		// 如果传入的handler是String类型，则表示是一个BeanName，从上下文获取
		Class<?> handlerType = (handler instanceof String ? obtainApplicationContext().getType((String) handler) : handler.getClass());

		if (handlerType != null) {
			Class<?> userType = ClassUtils.getUserClass(handlerType);
			// Method -> Method上对应的RequestMappingInfo
			// 又是非常熟悉的方法：MethodIntrospector.selectMethods
			// 它在我们招@EventListener、@Scheduled等注解方法时已经遇到过多次
			// 此处特别之处在于：getMappingForMethod属于一个抽象方法，由子类去决定它的寻找规则~~~~  什么才算作一个处理器方法
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					(MethodIntrospector.MetadataLookup<T>) method -> {
						try {
							return getMappingForMethod(method, userType);
						}
						catch (Throwable ex) {
							throw new IllegalStateException("Invalid mapping on handler class [" +
									userType.getName() + "]: " + method, ex);
						}
					});
			if (logger.isTraceEnabled()) {
				logger.trace(formatMappings(userType, methods));
			}
			else if (mappingsLogger.isDebugEnabled()) {
				mappingsLogger.debug(formatMappings(userType, methods));
			}
			// 把找到的Method  一个个遍历，注册进去
			methods.forEach((method, mapping) -> {
				/*
				 * 循环遍历所有的方法,进行注册
				 * handler为controller类实例, 每一个HandlerMethod中都维护着该实例, 因为在后面执行该方法时需要传入该实例
				 *
				 * 获取真实可执行的方法，因为上述查找逻辑在特殊情况下查找到的方法可能存在于代理上需要获取非代理方法作为可执行方法调用
				 */
				Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
				registerHandlerMethod(handler, invocableMethod, mapping);
			});
		}
	}

	private String formatMappings(Class<?> userType, Map<Method, T> methods) {
		String packageName = ClassUtils.getPackageName(userType);
		String formattedType = (StringUtils.hasText(packageName) ?
				Arrays.stream(packageName.split("\\."))
						.map(packageSegment -> packageSegment.substring(0, 1))
						.collect(Collectors.joining(".", "", "." + userType.getSimpleName())) :
				userType.getSimpleName());
		Function<Method, String> methodFormatter = method -> Arrays.stream(method.getParameterTypes())
				.map(Class::getSimpleName)
				.collect(Collectors.joining(",", "(", ")"));
		return methods.entrySet().stream()
				.map(e -> {
					Method method = e.getKey();
					return e.getValue() + ": " + method.getName() + methodFormatter.apply(method);
				})
				.collect(Collectors.joining("\n\t", "\n\t" + formattedType + ":" + "\n\t", ""));
	}

	/**
	 * Register a handler method and its unique mapping. Invoked at startup for
	 * each detected handler method.
	 * @param handler the bean name of the handler or the handler instance
	 * @param method the method to register
	 * @param mapping the mapping conditions associated with the handler method
	 * @throws IllegalStateException if another method was already registered
	 * under the same mapping
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * Create the HandlerMethod instance.
	 * @param handler either a bean name or an actual handler instance
	 * @param method the target method
	 * @return the created HandlerMethod
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		if (handler instanceof String) {
			return new HandlerMethod((String) handler,
					obtainApplicationContext().getAutowireCapableBeanFactory(),
					obtainApplicationContext(),
					method);
		}
		return new HandlerMethod(handler, method);
	}

	/**
	 * Extract and return the CORS configuration for the mapping.
	 */
	@Nullable
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
		return null;
	}

	/**
	 * Invoked after all handler methods have been detected.
	 * @param handlerMethods a read-only map with handler methods and mappings.
	 */
	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
		// Total includes detected mappings + explicit registrations via registerMapping
		int total = handlerMethods.size();
		if ((logger.isTraceEnabled() && total == 0) || (logger.isDebugEnabled() && total > 0) ) {
			logger.debug(total + " mappings in " + formatMappingName());
		}
	}


	// Handler method lookup

	/**
	 * Look up a handler method for the given request.
	 * 查找给定请求的处理程序方法
	 */
	@Override
	@Nullable
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		// lookupPath为请求路径信息（获取请求路径，作为查找路径）
		String lookupPath = initLookupPath(request);
		// 获取读锁
		this.mappingRegistry.acquireReadLock();
		try {
			// 获取HandlerMethod实例(不健全, 数据没有封装完成)
			// 委托给方法lookupHandlerMethod() 去找到一个HandlerMethod去最终处理
			HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
			// 重塑HandlerMethod实例  ==> 重点：（handlerMethod就是要执行的controller方法的封装）
			return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
		}
		finally {
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * Look up the best-matching handler method for the current request.
	 * If multiple matches are found, the best match is selected.
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param request0 the current request
	 * @return the best-matching handler method, or {@code null} if no match
	 * @see #handleMatch(Object, String, HttpServletRequest)
	 * @see #handleNoMatch(Set, String, HttpServletRequest)
	 */
	@Nullable
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
		// 匹配结果列表（Match是一个private class，内部就两个属性：T mapping和HandlerMethod handlerMethod）
		List<Match> matches = new ArrayList<>();
		// 由 映射处理器 映射出对应的MappingInfo信息, 可能会有多个
		// 至于为何是多值？可能URL都是/api/v1/hello  但是请求方式不一样（get、post、delete），还有可能是headers、consumes等等不一样，所以有可能是会匹配到多个MappingInfo的
		List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(lookupPath);
		// 如果有匹配的，就添加进匹配列表中
		if (directPathMatches != null) {
			// 将HandlerMethod实例与Request进行包装, 添加到matches集合中
			// 依赖于子类实现的抽象方法：getMatchingMapping()，根据URL、method、headers、consumes等等匹配到对应的MappingInfo，并加入到matches中
			addMatchingMappings(directPathMatches, matches, request);
		}
		// 还没有匹配，只能遍历所有的处理方法去找（至于为什么不直接没匹配上就404，我也不知道）
		if (matches.isEmpty()) {
			addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, request);
		}
		// 匹配结果不为空（也就是找到至少1个匹配）
		if (!matches.isEmpty()) {
			// 匹配结果比较器，直接使用匹配信息进行比较
			Match bestMatch = matches.get(0);
			// 对多个匹配结果进行排序
			if (matches.size() > 1) {
				// 依赖于子类实现的实现：getMappingComparator
				// 比如：`RequestMappingInfoHandlerMapping`的实现为先比较Method，patterns、params
				Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
				// 对所有符合条件的处理器进行排序
				matches.sort(comparator);
				// 获取matches集合中最合适的处理器（最佳匹配）
				bestMatch = matches.get(0);
				if (logger.isTraceEnabled()) {
					logger.trace(matches.size() + " matching mappings: " + matches);
				}
				if (CorsUtils.isPreFlightRequest(request)) {
					for (Match match : matches) {
						if (match.hasCorsConfig()) {
							return PREFLIGHT_AMBIGUOUS_MATCH;
						}
					}
				}
				else {
					Match secondBestMatch = matches.get(1);
					// 第一个元素和第二个元素进行比较（若相等，那就异常）
					if (comparator.compare(bestMatch, secondBestMatch) == 0) {
						// 结果为0，至少有2个最佳匹配
						Method m1 = bestMatch.getHandlerMethod().getMethod();
						Method m2 = secondBestMatch.getHandlerMethod().getMethod();
						String uri = request.getRequestURI();
						// 多个最佳匹配，抛出异常
						// 注意：这个是运行时的检查，在启动的时候是检查不出来的
						throw new IllegalStateException(
								"Ambiguous handler methods mapped for '" + uri + "': {" + m1 + ", " + m2 + "}");
					}
				}
			}
			// 把最最佳匹配的方法  放进request的属性里面
			request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, bestMatch.getHandlerMethod());
			// 它也往request的属性，塞了值
			handleMatch(bestMatch.mapping, lookupPath, request);
			// 返回最佳匹配对应的处理器方法
			return bestMatch.getHandlerMethod();
		}
		else {
			// 没有匹配结果，执行无处理器匹配方法，可执行一些特殊逻辑
			return handleNoMatch(this.mappingRegistry.getRegistrations().keySet(), lookupPath, request);
		}
	}

	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, HttpServletRequest request) {
		for (T mapping : mappings) {
			// 只有RequestMappingInfoHandlerMapping 实现了一句话：return info.getMatchingCondition(request);
			// 因此需要好好研究 RequestMappingInfo#getMatchingCondition()
			T match = getMatchingMapping(mapping, request);
			if (match != null) {
				/*
				 * 根据请求路径从mappingLookup集合中获取处理方法, 将Method封装到Match实例中，后面处理请求时会利用反射执行该方法
				 */
				matches.add(new Match(match, this.mappingRegistry.getRegistrations().get(mapping)));
			}
		}
	}

	/**
	 * Invoked when a matching mapping is found.
	 * 在找到匹配的映射时调用
	 * @param mapping the matching mapping
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param request the current request
	 */
	protected void handleMatch(T mapping, String lookupPath, HttpServletRequest request) {
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
	}

	/**
	 * Invoked when no matching mapping is not found.
	 * @param mappings all registered mappings
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param request the current request
	 * @throws ServletException in case of errors
	 */
	@Nullable
	protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, HttpServletRequest request)
			throws Exception {

		return null;
	}

	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return super.hasCorsConfigurationSource(handler) ||
				(handler instanceof HandlerMethod &&
						this.mappingRegistry.getCorsConfiguration((HandlerMethod) handler) != null);
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
				return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;
			}
			else {
				CorsConfiguration corsConfigFromMethod = this.mappingRegistry.getCorsConfiguration(handlerMethod);
				corsConfig = (corsConfig != null ? corsConfig.combine(corsConfigFromMethod) : corsConfigFromMethod);
			}
		}
		return corsConfig;
	}


	// Abstract template methods

	/**
	 * Whether the given type is a handler with handler methods.
	 * @param beanType the type of the bean being checked
	 * @return "true" if this a handler type, "false" otherwise.
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * Provide the mapping for a handler method. A method for which no
	 * mapping can be provided is not a handler method.
	 * @param method the method to provide a mapping for
	 * @param handlerType the handler type, possibly a sub-type of the method's
	 * declaring class
	 * @return the mapping, or {@code null} if the method is not mapped
	 */
	@Nullable
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * Extract and return the URL paths contained in the supplied mapping.
	 * @deprecated as of 5.3 in favor of providing non-pattern mappings via
	 * {@link #getDirectPaths(Object)} instead
	 */
	@Deprecated
	protected Set<String> getMappingPathPatterns(T mapping) {
		return Collections.emptySet();
	}

	/**
	 * Return the request mapping paths that are not patterns.
	 * @since 5.3
	 */
	protected Set<String> getDirectPaths(T mapping) {
		Set<String> urls = Collections.emptySet();
		for (String path : getMappingPathPatterns(mapping)) {
			if (!getPathMatcher().isPattern(path)) {
				urls = (urls.isEmpty() ? new HashSet<>(1) : urls);
				urls.add(path);
			}
		}
		return urls;
	}

	/**
	 * Check if a mapping matches the current request and return a (potentially
	 * new) mapping with conditions relevant to the current request.
	 * @param mapping the mapping to get a match for
	 * @param request the current HTTP servlet request
	 * @return the match, or {@code null} if the mapping doesn't match
	 */
	@Nullable
	protected abstract T getMatchingMapping(T mapping, HttpServletRequest request);

	/**
	 * Return a comparator for sorting matching mappings.
	 * The returned comparator should sort 'better' matches higher.
	 * @param request the current request
	 * @return the comparator (never {@code null})
	 */
	protected abstract Comparator<T> getMappingComparator(HttpServletRequest request);


	/**
	 * A registry that maintains all mappings to handler methods, exposing methods
	 * to perform lookups and providing concurrent access.
	 * <p>Package-private for testing purposes.
	 */
	class MappingRegistry {
		/**
		 * mapping对应的其MappingRegistration对象
		 */
		private final Map<T, MappingRegistration<T>> registry = new HashMap<>();

		/**
		 * .
		 * 维护url与请求信息(RequestMappingInfo)的映射关系，后面会根据Url找RequestMappingInfo, 再根据RequestMappingInfo找HandlerMethod对请求进行处理
		 *
		 * 保存着URL与匹配条件（mapping）的对应关系，当然这里的URL是pattern式的，可以使用通配符
		 * 	这里的Map不是普通的Map，而是MultiValueMap，它是个多值Map。其实它的value是一个list类型的值
		 * 	至于为何是多值？有这么一种情况  URL都是/api/v1/hello，但是有的是get post delete等方法，所以有可能是会匹配到多个MappingInfo的
		 *
		 */
		private final MultiValueMap<String, T> pathLookup = new LinkedMultiValueMap<>();

		/**
		 * 这个Map是Spring MVC4.1新增的（毕竟这个策略接口HandlerMethodMappingNamingStrategy在Spring4.1后才有,这里的name是它生成出来的）
		 * 保存着name和HandlerMethod的对应关系（一个name可以有多个HandlerMethod）
		 */
		private final Map<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();

		private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

		/**
		 * 读写锁，读写分离，高效率
		 */
		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

		/**
		 * Return all registrations.
		 * @since 5.3
		 */
		public Map<T, MappingRegistration<T>> getRegistrations() {
			return this.registry;
		}

		/**
		 * Return matches for the given URL path. Not thread-safe.
		 * 返回给定URL路径的匹配项。不是线程安全的
		 * @see #acquireReadLock()
		 */
		@Nullable
		public List<T> getMappingsByDirectPath(String urlPath) {
			return this.pathLookup.get(urlPath);
		}

		/**
		 * Return handler methods by mapping name. Thread-safe for concurrent use.
		 */
		public List<HandlerMethod> getHandlerMethodsByMappingName(String mappingName) {
			return this.nameLookup.get(mappingName);
		}

		/**
		 * Return CORS configuration. Thread-safe for concurrent use.
		 */
		@Nullable
		public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
			HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
			return this.corsLookup.get(original != null ? original : handlerMethod);
		}

		/**
		 * Acquire the read lock when using getMappings and getMappingsByUrl.
		 */
		public void acquireReadLock() {
			this.readWriteLock.readLock().lock();
		}

		/**
		 * Release the read lock after using getMappings and getMappingsByUrl.
		 */
		public void releaseReadLock() {
			this.readWriteLock.readLock().unlock();
		}

		/**
		 * 。
		 * 注册Mapping和handler 以及Method，此处上写锁保证线程安全
		 * @param mapping 1
		 * @param handler 2
		 * @param method 3
		 * @return : void
		 */
		public void register(T mapping, Object handler, Method method) {
			// 获取写锁
			this.readWriteLock.writeLock().lock();
			try {
				// 把类和方法封装为 HandlerMethod
				// 注意：都是new HandlerMethod()了一个新的出来
				HandlerMethod handlerMethod = createHandlerMethod(handler, method);
				// 保证方法映射唯一，如果一个相同的url对应多个handlerMethod则会抛出异常
				// 这里可能会出现常见的一个异常信息：Ambiguous mapping. Cannot map XXX
				validateMethodMapping(handlerMethod, mapping);


				Set<String> directPaths = AbstractHandlerMethodMapping.this.getDirectPaths(mapping);
				for (String path : directPaths) {
					/*
					 * pathLookup: 维护url与请求信息(RequestMappingInfo)的映射关系
					 * 后面会根据Url找RequestMappingInfo, 再根据RequestMappingInfo，找HandlerMethod对请求进行处理
					 */
					this.pathLookup.add(path, mapping);
				}

				String name = null;
				if (getNamingStrategy() != null) {
					name = getNamingStrategy().getName(handlerMethod, mapping);
					addMappingName(name, handlerMethod);
				}

				// 获取HandlerMethod对应的CORS配置
				CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
				if (corsConfig != null) {
					corsConfig.validateAllowCredentials();
					this.corsLookup.put(handlerMethod, corsConfig);
				}

				// 注册跨域配置
				this.registry.put(mapping,
						new MappingRegistration<>(mapping, handlerMethod, directPaths, name, corsConfig != null));
			}
			finally {
				// 释放锁
				this.readWriteLock.writeLock().unlock();
			}
		}

		private void validateMethodMapping(HandlerMethod handlerMethod, T mapping) {
			MappingRegistration<T> registration = this.registry.get(mapping);
			HandlerMethod existingHandlerMethod = (registration != null ? registration.getHandlerMethod() : null);
			if (existingHandlerMethod != null && !existingHandlerMethod.equals(handlerMethod)) {
				throw new IllegalStateException(
						"Ambiguous mapping. Cannot map '" + handlerMethod.getBean() + "' method \n" +
						handlerMethod + "\nto " + mapping + ": There is already '" +
						existingHandlerMethod.getBean() + "' bean method\n" + existingHandlerMethod + " mapped.");
			}
		}

		private void addMappingName(String name, HandlerMethod handlerMethod) {
			List<HandlerMethod> oldList = this.nameLookup.get(name);
			if (oldList == null) {
				oldList = Collections.emptyList();
			}

			for (HandlerMethod current : oldList) {
				if (handlerMethod.equals(current)) {
					return;
				}
			}

			List<HandlerMethod> newList = new ArrayList<>(oldList.size() + 1);
			newList.addAll(oldList);
			newList.add(handlerMethod);
			this.nameLookup.put(name, newList);
		}

		public void unregister(T mapping) {
			this.readWriteLock.writeLock().lock();
			try {
				MappingRegistration<T> registration = this.registry.remove(mapping);
				if (registration == null) {
					return;
				}

				for (String path : registration.getDirectPaths()) {
					List<T> mappings = this.pathLookup.get(path);
					if (mappings != null) {
						mappings.remove(registration.getMapping());
						if (mappings.isEmpty()) {
							this.pathLookup.remove(path);
						}
					}
				}

				removeMappingName(registration);

				this.corsLookup.remove(registration.getHandlerMethod());
			}
			finally {
				this.readWriteLock.writeLock().unlock();
			}
		}

		private void removeMappingName(MappingRegistration<T> definition) {
			String name = definition.getMappingName();
			if (name == null) {
				return;
			}
			HandlerMethod handlerMethod = definition.getHandlerMethod();
			List<HandlerMethod> oldList = this.nameLookup.get(name);
			if (oldList == null) {
				return;
			}
			if (oldList.size() <= 1) {
				this.nameLookup.remove(name);
				return;
			}
			List<HandlerMethod> newList = new ArrayList<>(oldList.size() - 1);
			for (HandlerMethod current : oldList) {
				if (!current.equals(handlerMethod)) {
					newList.add(current);
				}
			}
			this.nameLookup.put(name, newList);
		}
	}


	static class MappingRegistration<T> {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		private final Set<String> directPaths;

		@Nullable
		private final String mappingName;

		private final boolean corsConfig;

		public MappingRegistration(T mapping, HandlerMethod handlerMethod,
				@Nullable Set<String> directPaths, @Nullable String mappingName, boolean corsConfig) {

			Assert.notNull(mapping, "Mapping must not be null");
			Assert.notNull(handlerMethod, "HandlerMethod must not be null");
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
			this.directPaths = (directPaths != null ? directPaths : Collections.emptySet());
			this.mappingName = mappingName;
			this.corsConfig = corsConfig;
		}

		public T getMapping() {
			return this.mapping;
		}

		public HandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public Set<String> getDirectPaths() {
			return this.directPaths;
		}

		@Nullable
		public String getMappingName() {
			return this.mappingName;
		}

		public boolean hasCorsConfig() {
			return this.corsConfig;
		}
	}


	/**
	 * A thin wrapper around a matched HandlerMethod and its mapping, for the purpose of
	 * comparing the best match with a comparator in the context of the current request.
	 */
	private class Match {

		private final T mapping;

		private final MappingRegistration<T> registration;

		public Match(T mapping, MappingRegistration<T> registration) {
			this.mapping = mapping;
			this.registration = registration;
		}

		public HandlerMethod getHandlerMethod() {
			return this.registration.getHandlerMethod();
		}

		public boolean hasCorsConfig() {
			return this.registration.hasCorsConfig();
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	private class MatchComparator implements Comparator<Match> {

		private final Comparator<T> comparator;

		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}


	private static class EmptyHandler {

		@SuppressWarnings("unused")
		public void handle() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

}
