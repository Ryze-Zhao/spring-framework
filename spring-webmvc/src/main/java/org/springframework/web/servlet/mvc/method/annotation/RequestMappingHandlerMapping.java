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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;
import org.springframework.web.servlet.mvc.condition.CompositeRequestCondition;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Creates {@link RequestMappingInfo} instances from type and method-level
 * {@link RequestMapping @RequestMapping} annotations in
 * {@link Controller @Controller} classes.
 *
 * <p><strong>Deprecation Note:</strong></p> In 5.2.4,
 * {@link #setUseSuffixPatternMatch(boolean) useSuffixPatternMatch} and
 * {@link #setUseRegisteredSuffixPatternMatch(boolean) useRegisteredSuffixPatternMatch}
 * were deprecated in order to discourage use of path extensions for request
 * mapping and for content negotiation (with similar deprecations in
 * {@link org.springframework.web.accept.ContentNegotiationManagerFactoryBean
 * ContentNegotiationManagerFactoryBean}). For further context, please read issue
 * <a href="https://github.com/spring-projects/spring-framework/issues/24179">#24719</a>.
 *
 * 它也实现了MatchableHandlerMapping分支的接口,
 * EmbeddedValueResolverAware接口：说明要支持解析Spring的表达式
 *
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.1 Spring3.1才提供的这种注解扫描的方式的支持
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements MatchableHandlerMapping, EmbeddedValueResolverAware {

	private boolean useSuffixPatternMatch = false;

	private boolean useRegisteredSuffixPatternMatch = false;

	private boolean useTrailingSlashMatch = true;

	private Map<String, Predicate<Class<?>>> pathPrefixes = Collections.emptyMap();

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	@Nullable
	private StringValueResolver embeddedValueResolver;

	private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();


	/**
	 * Whether to use suffix pattern match (".*") when matching patterns to
	 * requests. If enabled a method mapped to "/users" also matches to "/users.*".
	 * <p>By default value this is set to {@code false}.
	 * <p>Also see {@link #setUseRegisteredSuffixPatternMatch(boolean)} for
	 * more fine-grained control over specific suffixes to allow.
	 * <p><strong>Note:</strong> This property is ignored when
	 * {@link #setPatternParser(PathPatternParser)} is configured.
	 * @deprecated as of 5.2.4. See class level note on the deprecation of
	 * path extension config options. As there is no replacement for this method,
	 * in 5.2.x it is necessary to set it to {@code false}. In 5.3 the default
	 * changes to {@code false} and use of this property becomes unnecessary.
	 */
	@Deprecated
	public void setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
	}

	/**
	 * Whether suffix pattern matching should work only against path extensions
	 * explicitly registered with the {@link ContentNegotiationManager}. This
	 * is generally recommended to reduce ambiguity and to avoid issues such as
	 * when a "." appears in the path for other reasons.
	 * <p>By default this is set to "false".
	 * <p><strong>Note:</strong> This property is ignored when
	 * {@link #setPatternParser(PathPatternParser)} is configured.
	 * @deprecated as of 5.2.4. See class level note on the deprecation of
	 * path extension config options.
	 */
	@Deprecated
	public void setUseRegisteredSuffixPatternMatch(boolean useRegisteredSuffixPatternMatch) {
		this.useRegisteredSuffixPatternMatch = useRegisteredSuffixPatternMatch;
		this.useSuffixPatternMatch = (useRegisteredSuffixPatternMatch || this.useSuffixPatternMatch);
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * <p>The default value is {@code true}.
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
		if (getPatternParser() != null) {
			getPatternParser().setMatchOptionalTrailingSeparator(useTrailingSlashMatch);
		}
	}

	/**
	 * Configure path prefixes to apply to controller methods.
	 * <p>Prefixes are used to enrich the mappings of every {@code @RequestMapping}
	 * method whose controller type is matched by the corresponding
	 * {@code Predicate}. The prefix for the first matching predicate is used.
	 * <p>Consider using {@link org.springframework.web.method.HandlerTypePredicate
	 * HandlerTypePredicate} to group controllers.
	 *
	 * 配置要应用于控制器方法的路径前缀
	 *      前缀用于enrich每个@RequestMapping方法的映射，至于匹不匹配由Predicate来决定  有种前缀分类的效果
	 *      推荐使用Spring5.1提供的类：org.springframework.web.method.HandlerTypePredicate
	 *
	 * @param prefixes a map with path prefixes as key
	 * @since 5.1 Spring5.1才出来的新特性
	 */
	public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
		this.pathPrefixes = (!prefixes.isEmpty() ?
				Collections.unmodifiableMap(new LinkedHashMap<>(prefixes)) :
				Collections.emptyMap());
	}

	/**
	 * The configured path prefixes as a read-only, possibly empty map.
	 *
	 * 注意pathPrefixes是只读的，因为上面Collections.unmodifiableMap了，有可能只是个空Map
	 * @since 5.1
	 */
	public Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}

	/**
	 * Set the {@link ContentNegotiationManager} to use to determine requested media types.
	 * If not set, the default constructor is used.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * Return the configured {@link ContentNegotiationManager}.
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void afterPropertiesSet() {
		// 创建映射信息构造器配置，用于构造映射信息RequestMappingInfo
		this.config = new RequestMappingInfo.BuilderConfiguration();
		// 是否添加"/"后缀，默认为true
		this.config.setTrailingSlashMatch(useTrailingSlashMatch());
		// 设置内容协商管理器组件（mediaType处理类：ContentNegotiationManager）
		this.config.setContentNegotiationManager(getContentNegotiationManager());

		if (getPatternParser() != null) {
			this.config.setPatternParser(getPatternParser());
			Assert.isTrue(!this.useSuffixPatternMatch && !this.useRegisteredSuffixPatternMatch,
					"Suffix pattern matching not supported with PathPatternParser.");
		}
		else {
			// 是否支持后缀补充，默认为true
			this.config.setSuffixPatternMatch(useSuffixPatternMatch());
			// 是否采用mediaType匹配模式，比如.json/.xml模式的匹配，默认为false
			this.config.setRegisteredSuffixPatternMatch(useRegisteredSuffixPatternMatch());
			//默认为AntPathMatcher，路径匹配校验器
			this.config.setPathMatcher(getPathMatcher());
		}
		// 执行处理器方法的初始化逻辑
		super.afterPropertiesSet();
	}


	/**
	 * Whether to use registered suffixes for pattern matching.
	 * @deprecated as of 5.2.4. See deprecation notice on
	 * {@link #setUseSuffixPatternMatch(boolean)}.
	 */
	@Deprecated
	public boolean useSuffixPatternMatch() {
		return this.useSuffixPatternMatch;
	}

	/**
	 * Whether to use registered suffixes for pattern matching.
	 * @deprecated as of 5.2.4. See deprecation notice on
	 * {@link #setUseRegisteredSuffixPatternMatch(boolean)}.
	 */
	@Deprecated
	public boolean useRegisteredSuffixPatternMatch() {
		return this.useRegisteredSuffixPatternMatch;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	/**
	 * Return the file extensions to use for suffix pattern matching.
	 * @deprecated as of 5.2.4. See class-level note on the deprecation of path
	 * extension config options.
	 */
	@Nullable
	@Deprecated
	@SuppressWarnings("deprecation")
	public List<String> getFileExtensions() {
		return this.config.getFileExtensions();
	}


	/**
	 * {@inheritDoc}
	 * <p>Expects a handler to have either a type-level @{@link Controller}
	 * annotation or a type-level @{@link RequestMapping} annotation.
	 *
	 * 判断该类，是否是一个handler（此处就体现出@Controller注解的特殊性了，这也是为何我们的XXXController用@Bean申明是无效的原因）
	 *
	 *
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
				AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
	}

	/**
	 * Uses method and type-level @{@link RequestMapping} annotations to create
	 * the RequestMappingInfo.
	 * 父类：AbstractHandlerMethodMapping#detectHandlerMethods中回去该类里面找所有的指定的方法
	 * 而什么叫指定的呢？就是靠这个来判定方法是否符合条件的
	 *
	 *
	 * @return the created RequestMappingInfo, or {@code null} if the method
	 * does not have a {@code @RequestMapping} annotation.
	 * @see #getCustomMethodCondition(Method)
	 * @see #getCustomTypeCondition(Class)
	 */
	@Override
	@Nullable
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		// 第一步：先拿到方法上的RequestMappingInfo
		RequestMappingInfo info = createRequestMappingInfo(method);
		if (info != null) {
			// 方法上有。在第二步：拿到类上的RequestMappingInfo
			RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
			// 如果方法RequestMappingInfo不为空，就查看类上，如果有的话要合并
			if (typeInfo != null) {
				// 倘若类上面也有，那就调用combine把两者结合
				// combine的逻辑基如下：
				//  names：name1+#+name2
				//  path：路径拼接起来作为全路径(容错了方法里没有/的情况)
				//  method、params、headers：取并集
				//  consumes、produces：以方法的为准，没有指定再取类上的
				//  custom：谁有取谁的。若都有：那就看custom具体实现的.combine方法去决定把  简单的说就是交给调用者了~~~
				info = typeInfo.combine(info);
			}
			// 在Spring5.1之后还要处理这个前缀匹配
			// 根据这个类，去找看有没有前缀  getPathPrefix()：entry.getValue().test(handlerType) = true算是匹配上了
			// 备注：也支持${os.name}这样的语法拿值，可以把前缀也写在专门的配置文件里面
			String prefix = getPathPrefix(handlerType);
			if (prefix != null) {
				// RequestMappingInfo.paths(prefix)  相当于统一在前面加上这个前缀
				info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
			}
		}
		return info;
	}

	@Nullable
	String getPathPrefix(Class<?> handlerType) {
		for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
			if (entry.getValue().test(handlerType)) {
				String prefix = entry.getKey();
				if (this.embeddedValueResolver != null) {
					prefix = this.embeddedValueResolver.resolveStringValue(prefix);
				}
				return prefix;
			}
		}
		return null;
	}

	/**
	 * Delegates to {@link #createRequestMappingInfo(RequestMapping, RequestCondition)},
	 * supplying the appropriate custom {@link RequestCondition} depending on whether
	 * the supplied {@code annotatedElement} is a class or method.
	 * 根据此方法/类，创建一个RequestMappingInfo
	 *
	 * @see #getCustomTypeCondition(Class)
	 * @see #getCustomMethodCondition(Method)
	 */
	@Nullable
	private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
		// 注意：此处使用的是findMergedAnnotation  这也就是为什么虽然@RequestMapping它并不具有继承的特性，但是你子类仍然有继承的效果的原因~~~~
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
		// 请注意：这里进行了区分处理  如果是Class的话  如果是Method的话
		// 这里返回的是一个condition 也就是看看要不要处理这个请求的条件
		RequestCondition<?> condition = (element instanceof Class ?
				getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));
		// 这个createRequestMappingInfo就是根据一个@RequestMapping以及一个condition创建一个
		// 显然如果没有找到此注解，这里就返回null了，表面这个方法啥的就不是一个info
		return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
	}

	/**
	 * Provide a custom type-level request condition.
	 * The custom {@link RequestCondition} can be of any type so long as the
	 * same condition type is returned from all calls to this method in order
	 * to ensure custom request conditions can be combined and compared.
	 * <p>Consider extending {@link AbstractRequestCondition} for custom
	 * condition types and using {@link CompositeRequestCondition} to provide
	 * multiple custom conditions.
	 * 返回的null。protected方法留给子类复写，子类可以据此自己定义一套自己的规则来限制匹配
	 * 它相当于在Spring MVC默认的规则的基础上，用户还可以自定义条件进行处理
	 *
	 *
	 * @param handlerType the handler type for which to create the condition
	 * @return the condition, or {@code null}
	 */
	@Nullable
	protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
		return null;
	}

	/**
	 * Provide a custom method-level request condition.
	 * The custom {@link RequestCondition} can be of any type so long as the
	 * same condition type is returned from all calls to this method in order
	 * to ensure custom request conditions can be combined and compared.
	 * <p>Consider extending {@link AbstractRequestCondition} for custom
	 * condition types and using {@link CompositeRequestCondition} to provide
	 * multiple custom conditions.
	 *
	 * 返回的null。protected方法留给子类复写，子类可以据此自己定义一套自己的规则来限制匹配
	 * 它相当于在Spring MVC默认的规则的基础上，用户还可以自定义条件进行处理
	 *
	 * @param method the handler method for which to create the condition
	 * @return the condition, or {@code null}
	 */
	@Nullable
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		return null;
	}

	/**
	 * Create a {@link RequestMappingInfo} from the supplied
	 * {@link RequestMapping @RequestMapping} annotation, which is either
	 * a directly declared annotation, a meta-annotation, or the synthesized
	 * result of merging annotation attributes within an annotation hierarchy.
	 * 根据@RequestMapping 创建一个RequestMappingInfo
	 *
	 */
	protected RequestMappingInfo createRequestMappingInfo(
			RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {

		RequestMappingInfo.Builder builder = RequestMappingInfo
				// 强大的地方在此处：path里竟然还支持/api/v1/${os.name}/hello 这样形式动态的获取值
				// 也就是说URL还可以从配置文件里面读取  Spring考虑很周到啊~~~
				// @GetMapping("/${os.name}/hello") // 支持从配置文件里读取此值  Windows 10
				.paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
				.methods(requestMapping.method())
				.params(requestMapping.params())
				.headers(requestMapping.headers())
				.consumes(requestMapping.consumes())
				.produces(requestMapping.produces())
				.mappingName(requestMapping.name());
		if (customCondition != null) {
			builder.customCondition(customCondition);
		}
		return builder.options(this.config).build();
	}

	/**
	 * Resolve placeholder values in the given array of patterns.
	 * @return a new array with updated patterns
	 */
	protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
		if (this.embeddedValueResolver == null) {
			return patterns;
		}
		else {
			String[] resolvedPatterns = new String[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
			}
			return resolvedPatterns;
		}
	}

	@Override
	public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
		super.registerMapping(mapping, handler, method);
		updateConsumesCondition(mapping, method);
	}

	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		super.registerHandlerMethod(handler, method, mapping);
		updateConsumesCondition(mapping, method);
	}

	private void updateConsumesCondition(RequestMappingInfo info, Method method) {
		ConsumesRequestCondition condition = info.getConsumesCondition();
		if (!condition.isEmpty()) {
			for (Parameter parameter : method.getParameters()) {
				MergedAnnotation<RequestBody> annot = MergedAnnotations.from(parameter).get(RequestBody.class);
				if (annot.isPresent()) {
					condition.setBodyRequired(annot.getBoolean("required"));
					break;
				}
			}
		}
	}

	@Override
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		Assert.isNull(getPatternParser(), "This HandlerMapping requires a PathPattern");
		RequestMappingInfo info = RequestMappingInfo.paths(pattern).options(this.config).build();
		RequestMappingInfo match = info.getMatchingCondition(request);
		return (match != null && match.getPatternsCondition() != null ?
				new RequestMatchResult(
						match.getPatternsCondition().getPatterns().iterator().next(),
						UrlPathHelper.getResolvedLookupPath(request),
						getPathMatcher()) : null);
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
		HandlerMethod handlerMethod = createHandlerMethod(handler, method);
		Class<?> beanType = handlerMethod.getBeanType();
		CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, CrossOrigin.class);
		CrossOrigin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);

		if (typeAnnotation == null && methodAnnotation == null) {
			return null;
		}

		CorsConfiguration config = new CorsConfiguration();
		updateCorsConfig(config, typeAnnotation);
		updateCorsConfig(config, methodAnnotation);

		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
			for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
				config.addAllowedMethod(allowedMethod.name());
			}
		}
		return config.applyPermitDefaultValues();
	}

	private void updateCorsConfig(CorsConfiguration config, @Nullable CrossOrigin annotation) {
		if (annotation == null) {
			return;
		}
		for (String origin : annotation.origins()) {
			config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
		}
		for (String patterns : annotation.originPatterns()) {
			config.addAllowedOriginPattern(resolveCorsAnnotationValue(patterns));
		}
		for (RequestMethod method : annotation.methods()) {
			config.addAllowedMethod(method.name());
		}
		for (String header : annotation.allowedHeaders()) {
			config.addAllowedHeader(resolveCorsAnnotationValue(header));
		}
		for (String header : annotation.exposedHeaders()) {
			config.addExposedHeader(resolveCorsAnnotationValue(header));
		}

		String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());
		if ("true".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(true);
		}
		else if ("false".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(false);
		}
		else if (!allowCredentials.isEmpty()) {
			throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
					"or an empty string (\"\"): current value is [" + allowCredentials + "]");
		}

		if (annotation.maxAge() >= 0 ) {
			config.setMaxAge(annotation.maxAge());
		}
	}

	private String resolveCorsAnnotationValue(String value) {
		if (this.embeddedValueResolver != null) {
			String resolved = this.embeddedValueResolver.resolveStringValue(value);
			return (resolved != null ? resolved : "");
		}
		else {
			return value;
		}
	}

}
