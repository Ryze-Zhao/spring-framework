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

package org.springframework.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationStartupAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Abstract implementation of the {@link org.springframework.context.ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors},
 * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as an "applicationEventMulticaster" bean
 * of type {@link org.springframework.context.event.ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading by extending
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overridden in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext {

	/**
	 * Name of the MessageSource bean in the factory.
	 * If none is supplied, message resolution is delegated to the parent.
	 * @see MessageSource
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * Name of the LifecycleProcessor bean in the factory.
	 * If none is supplied, a DefaultLifecycleProcessor is used.
	 * @see org.springframework.context.LifecycleProcessor
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	/**
	 * Name of the ApplicationEventMulticaster bean in the factory.
	 * If none is supplied, a default SimpleApplicationEventMulticaster is used.
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	/**
	 * Boolean flag controlled by a {@code spring.spel.ignore} system property that instructs Spring to
	 * ignore SpEL, i.e. to not initialize the SpEL infrastructure.
	 * <p>The default is "false".
	 */
	private static final boolean shouldIgnoreSpel = SpringProperties.getFlag("spring.spel.ignore");


	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		ContextClosedEvent.class.getName();
	}


	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Unique id for this context, if any. */
	private String id = ObjectUtils.identityToString(this);

	/** Display name. */
	private String displayName = ObjectUtils.identityToString(this);

	/** Parent context. */
	@Nullable
	private ApplicationContext parent;

	/** Environment used by this context. */
	@Nullable
	private ConfigurableEnvironment environment;

	/**
	 * BeanFactoryPostProcessors to apply on refresh.
	 * 应用BeanFactoryPostProcessors于刷新时
	 * */
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

	/** System time in milliseconds when this context started. */
	private long startupDate;

	/** Flag that indicates whether this context is currently active. */
	private final AtomicBoolean active = new AtomicBoolean();

	/** Flag that indicates whether this context has been closed already. */
	private final AtomicBoolean closed = new AtomicBoolean();

	/** Synchronization monitor for the "refresh" and "destroy". */
	private final Object startupShutdownMonitor = new Object();

	/** Reference to the JVM shutdown hook, if registered. */
	@Nullable
	private Thread shutdownHook;

	/** ResourcePatternResolver used by this context. */
	private ResourcePatternResolver resourcePatternResolver;

	/** LifecycleProcessor for managing the lifecycle of beans within this context. */
	@Nullable
	private LifecycleProcessor lifecycleProcessor;

	/** MessageSource we delegate our implementation of this interface to. */
	@Nullable
	private MessageSource messageSource;

	/** Helper class used in event publishing. */
	@Nullable
	private ApplicationEventMulticaster applicationEventMulticaster;

	/** Application startup metrics. **/
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	/**
	 * Statically specified listeners.
	 * 静态指定的侦听器。
	 *  */
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

	/** Local listeners registered before refresh. */
	@Nullable
	private Set<ApplicationListener<?>> earlyApplicationListeners;

	/** ApplicationEvents published before the multicaster setup. */
	@Nullable
	private Set<ApplicationEvent> earlyApplicationEvents;


	/**
	 * Create a new AbstractApplicationContext with no parent.
	 * 创建没有父级的新AbstractApplicationContext
	 */
	public AbstractApplicationContext() {
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	/**
	 * Create a new AbstractApplicationContext with the given parent context.
	 * 使用给定的父context创建新的AbstractApplicationContext。
	 * @param parent the parent context
	 */
	public AbstractApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Set the unique id of this application context.
	 * <p>Default is the object id of the context instance, or the name
	 * of the context bean if the context is itself defined as a bean.
	 * @param id the unique id of the context
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	/**
	 * Set a friendly name for this context.
	 * Typically done during initialization of concrete context implementations.
	 * <p>Default is the object id of the context instance.
	 */
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	/**
	 * Return a friendly name for this context.
	 * @return a display name for this context (never {@code null})
	 */
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * (that is, this context is the root of the context hierarchy).
	 */
	@Override
	@Nullable
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * Set the {@code Environment} for this application context.
	 * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
	 * default with this method is one option but configuration through {@link
	 * #getEnvironment()} should also be considered. In either case, such modifications
	 * should be performed <em>before</em> {@link #refresh()}.
	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * Return the {@code Environment} for this application context in configurable
	 * form, allowing for further customization.
	 * <p>If none specified, a default environment will be initialized via
	 * {@link #createEnvironment()}.
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardEnvironment}.
	 * <p>Subclasses may override this method in order to supply
	 * a custom {@link ConfigurableEnvironment} implementation.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * Return this context's internal bean factory as AutowireCapableBeanFactory,
	 * if already available.
	 * @see #getBeanFactory()
	 */
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * Return the timestamp (ms) when this context was first loaded.
	 */
	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be application-specific or a
	 * standard framework event)
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 */
	@Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 * @param eventType the resolved event type, if known
	 * @since 4.2
	 */
	protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
		Assert.notNull(event, "Event must not be null");

		// Decorate event as an ApplicationEvent if necessary
		ApplicationEvent applicationEvent;
		// 如果发布的事件是一个ApplicationEvent，直接发布
		if (event instanceof ApplicationEvent) {
			applicationEvent = (ApplicationEvent) event;
		}
		else {
			// 如果发布的事件不是一个ApplicationEvent，包装成一个PayloadApplicationEvent
			applicationEvent = new PayloadApplicationEvent<>(this, event);
			// 我们在应用程序中发布事件时，这个eventType必定为null
			if (eventType == null) {
				// 获取对应的事件类型
				eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
			}
		}

		// Multicast right now if possible - or lazily once the multicaster is initialized
		// 我们自定义调用时，这个earlyApplicationEvents必定为null
		// 如果早期事件已经被初始化了，那就先放进早期事件里，否则就直接发送事件了
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		}
		else {
			// 获取事件广播器，发布对应的事件
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}

		// Publish event via parent context as well...
		// 如果存在父容器，那父容器也会发送一个事件
		if (this.parent != null) {
			if (this.parent instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
			}
			else {
				this.parent.publishEvent(event);
			}
		}
	}

	/**
	 * Return the internal ApplicationEventMulticaster used by the context.
	 * @return the internal ApplicationEventMulticaster (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "applicationStartup should not be null");
		this.applicationStartup = applicationStartup;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * Return the internal LifecycleProcessor used by the context.
	 * 返回上下文使用的内部生命周期处理器。
	 * @return the internal LifecycleProcessor (never {@code null}) 内部生命周期处理器（从不为空）
	 * @throws IllegalStateException if the context has not been initialized yet    如果上下文尚未初始化
	 */
	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " +
					"call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	/**
	 * Return the ResourcePatternResolver to use for resolving location patterns
	 * into Resource instances. Default is a
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
	 * supporting Ant-style location patterns.
	 * <p>Can be overridden in subclasses, for extended resolution strategies,
	 * for example in a web environment.
	 * <p><b>Do not call this when needing to resolve a location pattern.</b>
	 * Call the context's {@code getResources} method instead, which
	 * will delegate to the ResourcePatternResolver.
	 * @return the ResourcePatternResolver for this context
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Set the parent of this application context.
	 * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
	 * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
	 * this (child) application context environment if the parent is non-{@code null} and
	 * its environment is an instance of {@link ConfigurableEnvironment}.
	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		this.parent = parent;
		if (parent != null) {
			Environment parentEnvironment = parent.getEnvironment();
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}

	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}

	/**
	 * Return the list of BeanFactoryPostProcessors that will get applied to the internal BeanFactory.
	 * 返回将应用于内部BeanFactory的BeanFactoryPostProcessors列表。
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}

	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		this.applicationListeners.add(listener);
	}

	/**
	 * Return the list of statically specified ApplicationListeners.
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {

		// 锁：避免当前 refresh() 未结束，又重新启动或销毁容器的操作，导致出现问题
		synchronized (this.startupShutdownMonitor) {

			//	不重要！启动记录信息
			StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");

			// Prepare this context for refreshing.
			/*
			 * <Spring分析点38-1> 刷新前预处理
			 *
			 * 用于设置准备刷新前的一些参数:程序启动标志位/上下文拓展资源加载/上下文环境准备情况验证/监听器监听事件容器初始化准备等
			 */
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			/*
			 * <Spring分析点38-2> 获取BeanFactory工厂 及 装载BeanDefinition 信息【在创建IOC容器时创建的DefaultListableBeanFactory】
			 *
			 * <Spring分析点38-2.1> 获取BeanFactory,内部调用refreshBeanFactory()和getBeanFactory()均由子类实现，告知子类刷新 `BeanFactory` (设置序列号ID--> 参考GenericApplicationContext)
			 * <Spring分析点38-2.2> 将配置文件、注解 解析为各个 BeanDefinition ，并注册到 BeanFactory 中，这一步 Bean 还没有初始化，只是将配置信息都提取成 BeanDefinition，
			 * 并将这些 BeanDefinition 都保存到了 Map 中(核心是一个 key为beanName，value为 BeanDefinition 的 Map)
			 */
			/*
			 * obtainFreshBeanFactory()方法中根据实现类的不同调用不同的refreshBeanFactory()方法
			 *
			 * <-----  注解模式  ----->
			 * 1. 如果是使用 AnnotationConfigApplicationContext 来初始化环境调用的是GenericApplicationContext#refreshBeanFactory()方法
			 * {@link GenericApplicationContext#refreshBeanFactory()}在该方法中只是对beanFactory的一些变量进行设置
			 *
			 * <----- XML配置模式  ----->
			 *  2. 如果是使用 ClassPathXmlApplicationContext 来初始化环境，调用的是{@link AbstractRefreshableApplicationContext#refreshBeanFactory()}
			 * 由于还没有对beanFactory进行初始化，所以在该方法中，完成了对beanFactory的初始化操作，并对设置的资源位置进行扫描，解析
			 * 注意:此方法是使用 ClassPathXmlApplicationContext 来初始化上下文是解析注册bean的重要入口
			 */
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			/*
			 * <Spring分析点38-3> 预处理 BeanFactory ，对其进行 填充 预准备工作(初始化 `BeanFactory` ,设置基础属性)；
			 */

			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				/*
				 * 前置知识点： BeanFactoryPostProcessor ，如果有 Bean 实现了此接口，那么在容器初始化以后，Spring 会负责调用里面的 postProcessBeanFactory 方法
				 *
				 *
				 * <Spring分析点38-4> BeanFactory后置处理工作
				 *
				 * 提供给子类的扩展点：
				 *  因此具体的子类可以在这步的时候添加一些特殊的 BeanFactoryPostProcessor 的实现类 对BeanDefinition进行修改
				 *
				 * 注意：
				 *  所有 Bean 信息都加载、注册为BeanDefinition，当前这些 Bean 仍未初始化
				 * 注意：
				 *  BeanFactoryPostProcessor：通常用于处理 BeanDefinition （BeanFactory处理工作前后对BeanDefinition进行操作）
				 *  InstantiationAwareBeanPostProcessor（也是BeanPostProcessor子类）：通常用于处理 Bean （Bean 实例化阶段，对Bean进行操作）
				 *  BeanPostProcessor：通常用于处理 Bean （Bean 初始化阶段，对Bean进行操作）
				 *
				 */
				postProcessBeanFactory(beanFactory);

				//	不重要！记录beanPostProcess信息
				StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");

				// Invoke factory processors registered as beans in the context.
				/*
				 * <Spring分析点38-5> 执行已被注册的 BeanFactoryPostProcessor接口各实现类#postProcessBeanFactory(factory) 方法
				 *
				 * <-----  注解模式  ----->
				 * 1. 如果是使用 AnnotationConfigApplicationContext 来初始化环境(该方法是解析注册bean的重要入口)
				 * 在该方法中执行了 {@link org.springframework.context.annotation.ConfigurationClassPostProcessor#processConfigBeanDefinitions} 方法
				 * 对配置类进行解析(如果该注解bean是配置类则在这个方法里完成了包扫描操作)
				 * 注意:
				 *  1.1. 一般情况下, 此时beanFactory中只注册了这一个BeanFactoryPostProcessor类-->ConfigurationClassPostProcessor
				 *     在{@link AnnotationConfigUtils#registerAnnotationConfigProcessors(org.springframework.beans.factory.support.BeanDefinitionRegistry)}中注册的
				 *  1.2. 注解模式下，此时BeanDefinitionMap中含有6个Spring内部处理器类, 其中通过ConfigurationClassPostProcessor类来解析配置类，完成包扫描,bean注册等操作
				 *     然而, 在xml配置模式下, 此时BeanDefinitionMap只有自定义配置的BeanDefinition信息
				 *
				 * <----- XML配置模式  ----->
				 * 2. 如果是使用 ClassPathXmlApplicationContext 来初始化环境
				 *  默认情况下, 此时BeanDefinitionMap只有自定义配置的BeanDefinition信息, 并没有任何Spring内部所定义的BeanFactory后处理器
				 *  除非用户自定义了BeanFactory后处理器, 需要对BeanFactory进行修改, 那么才会执行对应后处理器里面的方法;
				 *
				 */
				invokeBeanFactoryPostProcessors(beanFactory);


				// Register bean processors that intercept bean creation.
				/*
				 * <Spring分析点38-6> 注册 BeanPostProcessor接口各实现类
				 *
				 * 1.分类
				 * 1.1.特殊的子类 InstantiationAwareBeanPostProcessor 作用为：Bean的后置处理器，拦截Bean的实例化
				 * 1.2.普通的 BeanPostProcessor 作用为：Bean的后置处理器，拦截Bean的初始化
				 * 2.接口重要方法：
				 * 2.1.InstantiationAwareBeanPostProcessor：postProcessBeforeInstantiation（Bean 实例化之前） 和 postProcessAfterInstantiation（Bean 实例化之后）
				 * 2.2.BeanPostProcessor：postProcessBeforeInitialization（Bean 初始化之前） 和 postProcessAfterInitialization（Bean 初始化之后）
				 *
				 *
				 * 注意：
				 *  BeanFactoryPostProcessor：通常用于处理 BeanDefinition （BeanFactory处理工作前后对BeanDefinition进行操作）
				 *  InstantiationAwareBeanPostProcessor（也是BeanPostProcessor子类）：通常用于处理 Bean （Bean 实例化阶段，对Bean进行操作）
				 *  BeanPostProcessor：通常用于处理 Bean （Bean 初始化阶段，对Bean进行操作）
				 */
				registerBeanPostProcessors(beanFactory);
				beanPostProcess.end();

				// Initialize message source for this context.
				/*
				 * <Spring分析点38-7> 初始化 MessageSource 组件（做国际化功能；消息绑定，消息解析）
				 */
				initMessageSource();

				// Initialize event multicaster for this context.
				/*
				 * <Spring分析点38-8> 初始化当前 ApplicationContext事件广播器（ApplicationEventMulticaster）
				 *
				 * 稍后注册监听器时会用到
				 */
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				/*
				 * <Spring分析点38-9> 在特定 ApplicationContext 的子类中触发某些特殊的Bean初始化
				 *
				 * 提供给子类的扩展点：当有子类重写这个方法后，在容器刷新的时候可以执行子类自定义逻辑；
				 * 从方法名就可以知道，典型的模板方法(钩子方法)，具体的子类可以在这里初始化一些特殊的 Bean（在初始化 singleton beans 之前）
				 * 在此处AbstractApplicationContext.onRefresh 是一个空方法
				 */
				onRefresh();

				// Check for listener beans and register them.
				/*
				 * <Spring分析点38-10> 注册 ApplicationListener（用于分发后期产生的事件，也可能没有事件）
				 *
				 * 注：监听器 需要实现 ApplicationListener 接口。
				 */
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				/*
				 * <Spring分析点38-11> 初始化 所有 `非懒加载` 且 `未被初始化` 的 `singleton` 的 Bean；（重点）
				 *
				 * 注：该方法完成后，可以认为符合条件的Bean完成初始并实例（其他类型的Bean是未完成哦）
				 */
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				// <Spring分析点38-12> 完成BeanFactory的初始化创建工作，并发布 ContextRefreshedEvent 事件，ApplicationContext 初始化完成
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				// 销毁已经初始化的 singleton 的 Beans，以免有些 bean 一直占用资源
				destroyBeans();

				// Reset 'active' flag.
				// 取消Refresh,刷新this.active属性为false
				cancelRefresh(ex);

				// Propagate exception to caller.
				// 把异常往外抛
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we might not ever need metadata for singleton beans anymore...
				// 清理缓存信息
				resetCommonCaches();
				contextRefresh.end();
			}
		}
	}

	/**
	 * Prepare this context for refreshing, setting its startup date and active flag as well as performing any initialization of property sources.
	 * 准备此上下文以进行刷新，设置其启动日期和活动标志，以及执行属性源的任何初始化。
	 */
	protected void prepareRefresh() {
		// Switch to active.
		// 设置基础属性：设置当前状态为活跃/启动、记录启动时间等
		this.startupDate = System.currentTimeMillis();
		this.closed.set(false);
		this.active.set(true);

		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			}
			else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		// Initialize any placeholder property sources in the context environment.
		// 交由子类实现，用于加载初始化资源属性.
		initPropertySources();

		// Validate that all properties marked as required are resolvable:see ConfigurablePropertyResolver#setRequiredProperties
		// 校验所需要的环境变量是否已经加载进来
		getEnvironment().validateRequiredProperties();

		// Store pre-refresh ApplicationListeners...
		// 存储预刷新时ApplicationListener
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		}
		else {
			// Reset local application listeners to pre-refresh state.
			// 将本地应用程序 Linsteners监听器 重置为预刷新状态。
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// Allow for the collection of early ApplicationEvents,to be published once the multicaster is available...
		// 创建ApplicationEvent监听事件容器，因为这时候还没有注册监听器，不能发布事件
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}

	/**
	 * <p>Replace any stub property sources with actual instances.
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
	 */
	protected void initPropertySources() {
		// For subclasses: do nothing by default.
	}

	/**
	 * Tell the subclass to refresh the internal bean factory.
	 * 告诉子类刷新内部 `BeanFactory` 。
	 * @return the fresh BeanFactory instance
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 */
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		/*
		 * 此处根据实现类的不同调用不同的refreshBeanFactory()方法
		 *
		 * <----- 注解配置模式  ----->
		 * 1. 如果是使用 AnnotationConfigApplicationContext 来初始化环境，调用的是GenericApplicationContext#refreshBeanFactory()方法
		 * {@link GenericApplicationContext#refreshBeanFactory()}，在该方法中只是对beanFactory的一些变量进行设置
		 *
		 * <----- XML配置模式  ----->
		 * 2. 如果是使用ClassPathXmlApplicationContext来初始化环境，用的是{@link AbstractRefreshableApplicationContext#refreshBeanFactory()}
		 * 由于还没有对beanFactory进行初始化，所以在该方法中，完成了对beanFactory的初始化操作
		 *
		 * 注意:两种方式都使用的是间接继承AbstractApplicationContext这个抽象类，并重写refreshBeanFactory()方法
		 */
		refreshBeanFactory();
		/*
		 * 返回已经刷新完成的bean工厂
		 * <----- 注解配置模式  ----->
		 * 1. GenericApplicationContext 继承了 AbstractApplicationContext 并重写了getBeanFactory()方法;
		 * 2. 所以此处调用的是 GenericApplicationContext#getBeanFactory()方法，将GenericApplicationContext中
		 *      DefaultListableBeanFactory 类型的 beanFactory 以 ConfigurableListableBeanFactory 类型返回;
		 * 3. DefaultListableBeanFactory 实现了 ConfigurableListableBeanFactory 接口{@link GenericApplicationContext#getBeanFactory()}
		 *
		 * <----- XML配置模式  ----->
		 * 返回已经刷新完成bean工厂
		 * {@link AbstractRefreshableApplicationContext#getBeanFactory()}
		 *
		 */
		return getBeanFactory();
	}

	/**
	 * Configure the factory's standard context characteristics, such as the context's ClassLoader and post-processors.
	 * 配置工厂的标准上下文特征，例如上下文的 ClassLoader 和 PostProcessors(后处理器)
	 * @param beanFactory the BeanFactory to configure  要配置的beanFactory
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// Tell the internal bean factory to use the context's class loader etc.

		/*
		 * <Spring分析点38-3.1> ClassLoader
		 * <Spring分析点38-3.2> Spel(Spring表达式)解析器
		 * <Spring分析点38-3.3> 属性编辑器(自定义属性覆盖默认属性)、ResourceEditorRegistrar(初始化前执行一些Aware即invokeAwareInterfaces方法)
		 * <Spring分析点38-3.4> 添加系统BeanPostProcessor(主要是：ApplicationContextAwareProcessor 和 ApplicationListenerDetector(Bean初始化后执行，判断是否是单例监听器加到上下文中))
		 * <Spring分析点38-3.5> 忽略一些系统级接口装配依赖(因为在 ApplicationContextAwareProcessor 中已经完成了手动注入)
		 * <Spring分析点38-3.6> 注入一些不能自动创建的Bean依赖(BeanFactory、ResourceLoader(加载资源文件)、ApplicationEventPublisher(事件发布类)、ApplicationContext(上下文))
		 * <Spring分析点38-3.7> 检测LoadTimeWeaver并准备编织（如果发现）,比如 AspectJ静态代理支持
		 * <Spring分析点38-3.8> 注册默认的environment beans并设置
		 */


		// <Spring分析点38-3.1> 设置内部 BeanFactory 的ClassLoader
		beanFactory.setBeanClassLoader(getClassLoader());
		// <Spring分析点38-3.2> 判断并设置加载 BeanFactory 的Spel表达式解析器（默认加载）
		//   默认可以使用#{bean.xxx}的形式来调用相关属性值
		if (!shouldIgnoreSpel) {
			beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		}
		// <Spring分析点38-3.3> 为 BeanFactory 增加了一个默认的属性编辑器PropertyEditor,这个主要是对bean的属性 自定义 的一个工具
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// Configure the bean factory with context callbacks.
		// <Spring分析点38-3.4>  添加默认注册Bean前加入BeanPostProcessor用于Bean初始化前进行操作: 如果Bean实现了某个Aware则调用对应方法.
		// 可查看ApplicationContextAwareProcessor.ApplicationContextAwareProcessor方法，自定义Bean可进行拓展
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		// <Spring分析点38-3.5> 忽略一些系统级接口依赖，这些方法在上面的ApplicationContextAwareProcessor中检查回调中会执行.
		// 忽略自动装配依赖的接口，因为在doCreateBean时，会在ApplicationContextAwareProcessor这个PostProcessor中已经手工注入；注意忽略的是XXXAware里的setXXX的方法，而不是XXXAware本身
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationStartupAware.class);

		// BeanFactory interface not registered as resolvable type in a plain factory. MessageSource registered (and found for autowiring) as a bean.
		// <Spring分析点38-3.6> 设置注册不能自动创建的Bean依赖，因此我们可以直接注入这几个组件： BeanFactory、ResourceLoader、ApplicationEventPublisher、ApplicationContext
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// Register early post-processor for detecting inner beans as ApplicationListeners.
		// <Spring分析点38-3.4> 添加Bean初始化后操作，如果有该单例监听器Bean就加入到上下文监听器容器中
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// Detect a LoadTimeWeaver and prepare for weaving, if found.
		// <Spring分析点38-3.7> 检测LoadTimeWeaver并准备编织（如果发现）,比如 AspectJ静态代理支持
		if (!NativeDetector.inNativeImage() && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			// 为类型匹配设置临时类加载器
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// Register default environment beans.
		// <Spring分析点38-3.8> 检查并加入一些默认的初始化环境Bean到 一级缓存 singletonObjects 中

		// 注册 environment 组件，类型是【ConfigurableEnvironment】
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		// 注册 systemProperties 组件，类型是【Map<String, Object>】
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		// 注册 systemEnvironment 组件，类型是【Map<String, Object>】
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
		// 注册 applicationStartup 组件，类型是【ApplicationStartup】
		if (!beanFactory.containsLocalBean(APPLICATION_STARTUP_BEAN_NAME)) {
			beanFactory.registerSingleton(APPLICATION_STARTUP_BEAN_NAME, getApplicationStartup());
		}
	}

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for registering special
	 * BeanPostProcessors etc in certain ApplicationContext implementations.
	 * 在标准初始化之后修改 `ApplicationContext` 的内部 `BeanFactory` 。所有BeanDefinition都已加载，但尚未实例化任何bean。
	 * 这允许在某些ApplicationContext实现中注册特殊的BeanPostProcessor等。
	 * @param beanFactory the bean factory used by the application context   `ApplicationContext` 使用的 `BeanFactory`
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	/**
	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans, respecting explicit order if given.
	 * <p>Must be called before singleton instantiation.
	 * 实例化并调用所有注册的BeanFactoryPostProcessor bean，如果给定，则遵循显式顺序。 必须在单例实例化之前调用。
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// Spring扫描加载完毕所有的BeanDefinition(扫描出来的各种注解和config配置的BeanDefinition)后，
		// 调用执行 自动加载和手动添加 的 BeanDefinitionRegistryPostProcessor接口 和 BeanFactoryPostProcessor接口 的实现类
		// 注：BeanDefinitionRegistryPostProcessor接口 继承 BeanFactoryPostProcessor接口

		// 自动加载：由Spring自己扫描添加的
		// 手动添加：手动调用 AnnotationConfigApplicationContext.addBeanFactoryPostProcesor()添加的
		// 注意:getBeanFactoryPostProcessors()是获取手动添加给Spring的 BeanFactoryPostProcessor

		// 例如：自己写的类，若添加了@Component，那么getBeanFactoryPostProcessors()就不会获取到对应的类，因为这种情况属于Spring扫描添加（即：自动加载）
		// 例如：自己写的类，若不添加了@Component，而使用AnnotationConfigApplicationContext.addBeanFactoryPostProcesor()添加的，那么getBeanFactoryPostProcessors()就会获取到对应的类（属于手动添加）,
		//      则会在 ConfigurationClassPostProcessor#processConfigBeanDefinitions() 方法之前执行
		 PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime(e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
		// 检测LoadTimeWeaver并准备编织（如果同时发现）（例如，通过ConfigurationClassPostProcessor注册的@Bean方法）
		if (!NativeDetector.inNativeImage() && beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

	/**
	 * Instantiate and register all BeanPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before any instantiation of application beans.
	 * 实例化并调用已经注入的 BeanPostProcessor
	 * 必须在应用中 bean 实例化之前调用
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	}

	/**
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 * 初始化消息源。如果在此上下文中未定义，请使用父级。
	 */
	protected void initMessageSource() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 判断是否已经存在名为 “messageSource” 的 Bean 了（初次进入时，若没自定义就一般没有，因此走else）
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			// 从容器里拿出这个 messageSource
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			// 设置父属性
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {
			// Use empty MessageSource to be able to accept getMessage calls.
			DelegatingMessageSource dms = new DelegatingMessageSource();
			// 其实就是获取到父容器的 messageSource 字段（否则就是 getParent() 上下文自己）
			dms.setParentMessageSource(getInternalParentMessageSource());
			// 给当前的 messageSource 赋值
			this.messageSource = dms;
			// 把 messageSource 作为一个单例的 Bean 注册进 beanFactory 工厂里
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * Initialize the ApplicationEventMulticaster.Uses SimpleApplicationEventMulticaster if none defined in the context.
	 * 事件广播器用于Spring事件通知机制，监听器通过指定监听事件，当广播器广播该事件时会执行对应监听器方法.
	 * 该方法表示如果有自定义广播器则使用自定义广播器没有则创建一个SimpleApplicationEventMulticaster.可自定义拓展让广播器监听事件异步执行
	 * 保存事件和对应监听器列表映射，发布事件后会找到该事件的所有监听器.如果由线程池则异步执行.没有则同步执行
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	protected void initApplicationEventMulticaster() {
		// 获取 beanFactory
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 是否包含指定名称 applicationEventMulticaster 的 Bean
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			// 有就从容器中获取赋值
			this.applicationEventMulticaster = beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		else {
			// 如果不包含applicationEventMulticaster 的 Bean，那么就使用  SimpleApplicationEventMulticaster 创建一个 事件广播器
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			// 将创建的 ApplicationEventMulticaster 添加到 BeanFactory 中， 其他组件就可以自动注入了
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
						"[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Initialize the LifecycleProcessor. Uses DefaultLifecycleProcessor if none defined in the context.
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 * 初始化生命周期处理器。如果上下文中未定义DefaultLifecycleProcessor，则使用DefaultLifecycleProcessor。
	 */
	protected void initLifecycleProcessor() {
		// 当 ApplicationContext 启动或停止时，它会通过 LifecycleProcessor 来与所有声明的 Bean 的周期做状态更新，而在 LifecycleProcessor 的使用前首先需要初始化
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 我们自己没自定义就一定是走这个DefaultLifecycleProcessor（即：else部分）
		// 当然，我们自定义的话最好也是继承默认的 DefaultLifecycleProcessor 来定义，因为他是让Bean生命周期随上下文一致的保证
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			// 如果工厂里已经存在 LifecycleProcessor，那就拿出来，把值放上去 this.lifecycleProcessor
			this.lifecycleProcessor =
					beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		}
		else {
			// 一般情况下，都会注册上这个默认的处理器 DefaultLifecycleProcessor
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			defaultProcessor.setBeanFactory(beanFactory);
			this.lifecycleProcessor = defaultProcessor;
			// 直接注册成单例 Bean 进去容器里
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
						"[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Template method which can be overridden to add context-specific refresh work.
	 * Called on initialization of special beans, before instantiation of singletons.
	 * <p>This implementation is empty.
	 * @throws BeansException in case of errors
	 * @see #refresh()
	 */
	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

	/**
	 * Add beans that implement ApplicationListener as listeners. Doesn't affect other listeners, which can be added without being beans.
	 * 将早期加入的系ApplicationListener加入广播器中.常规的监听器Bean则先加入名字等到后面和其他Bean一起创建.从BeanFactory中获取即可.并发布早期Application事件
	 */
	protected void registerListeners() {
		// Register statically specified listeners first.
		// 将Application启动时加载的Listeners加入广播器中
		// 这里的 applicationListeners 是需要我们手动调用 AbstractApplicationContext.addApplicationListener 方法才会有内容
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			// getApplicationEventMulticaster() 就是获取之前步骤初始化的 applicationEventMulticaster
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans uninitialized to let post-processors apply to them!
		// 从容器中获取所有的 ApplicationListener
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// Publish early application events now that we finally have a multicaster...
		// 广播早期Application事件（之前步骤产生的），并且将 earlyApplicationEvents 设置为空
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null;
		if (!CollectionUtils.isEmpty(earlyEventsToProcess)) {
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

	/**
	 * Finish the initialization of this context's bean factory, initializing all remaining singleton beans.
	 * 完成此上下文 `BeanFactory` 的初始化，初始化所有剩余的单例bean。
	 */
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// Initialize conversion service for this context.
		// 初始化此上下文的converter(可以自定义哦，可以看 Converter 体系 文章)
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// Register a default embedded value resolver if no BeanFactoryPostProcessor
		// (such as a PropertySourcesPlaceholderConfigurer bean) registered any before:
		// at this point, primarily for resolution in annotation attribute values.
		// 如果beanFactory之前没有注册嵌入值解析器，则注册默认的嵌入值解析器：主要用于注解属性值的解析
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		// 初始化LoadTimeWeaverAware Bean实例对象
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// stop using the temporary classloader for type matching.
		// 停止使用临时ClassLoader进行类型匹配
		beanFactory.setTempClassLoader(null);

		// Allow for caching all bean definition metadata, not expecting further changes.
		// 冻结所有BeanDefinition，注册的BeanDefinition不再被修改或进一步后处理，因为马上进行 Bean实例化 对象了
		beanFactory.freezeConfiguration();

		// Instantiate all remaining (non-lazy-init) singletons.
		// 实例化所有剩余（非懒加载）单例对象
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * Finish the refresh of this context, invoking the LifecycleProcessor's onRefresh() method and publishing the
	 * {@link org.springframework.context.event.ContextRefreshedEvent}.
	 * 完成此上下文的刷新，调用LifecycleProcessor的onRefresh()方法并发布ContextRefreshedEvent
	 */
	@SuppressWarnings("deprecation")
	protected void finishRefresh() {
		// Clear context-level resource caches (such as ASM metadata from scanning).
		// Spring5.0 之后才有的方法
		// 清除为上下文创建初始化准备的资源文件数据信息缓存.比如：resourceCaches、ASM的元数据
		clearResourceCaches();

		// Initialize lifecycle processor for this context.
		// 初始化所有的 LifecycleProcessor,一般是加载DefaultLifecycleProcessor（为此上下文初始化生命周期处理器）
		// 在 Spring 中还提供了 Lifecycle 接口， Lifecycle 中包含 start/stop 方法，实现此接口后 Spring 会保证在启动的时候调用其 start 方法开始生命周期，
		// 并在 Spring 关闭的时候调用 stop 方法来结束生命周期，通常用来配置后台程序，在启动后一直运行（如对 MQ 进行轮询等）
		// ApplicationContext 的初始化最后正是保证了这一功能的实现
		initLifecycleProcessor();

		// Propagate refresh to lifecycle processor first.
		// 调用上面初始化好的LifecycleProcessor#onRefresh()，实现了SmartLifecycle接口的start方法启动对应Bean生命周期(随着Application启动而启动，关闭而关闭)
		getLifecycleProcessor().onRefresh();

		// Publish the final event.
		// 发布 Context 刷新完毕事件到相应的监听器(事件对象:ContextRefreshedEvent)
		publishEvent(new ContextRefreshedEvent(this));

		// Participate in LiveBeansView MBean, if active.
		// 设置JMX则执行，和 MBeanServer 和 MBean 有关的。相当于把当前容器上下文，注册到 MBeanServer 里面去
		// 这样子，MBeanServer 持有了容器的引用，就可以拿到容器的所有内容了，也就让 Spring 支持到了 MBean 的相关功能
		if (!NativeDetector.inNativeImage()) {
			LiveBeansView.registerApplicationContext(this);
		}
	}

	/**
	 * Cancel this context's refresh attempt, resetting the {@code active} flag after an exception got thrown.
	 * 取消此上下文的刷新尝试，在引发异常后重置活动标志。
	 * @param ex the exception that led to the cancellation 导致取消的例外情况
	 */
	protected void cancelRefresh(BeansException ex) {
		this.active.set(false);
	}

	/**
	 * Reset Spring's common reflection metadata caches, in particular the
	 * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
	 * and {@link CachedIntrospectionResults} caches.
	 * 重置Spring的公共反射元数据缓存，特别是ReflectionUtils、AnnotationUtils、ResolvableType和CachedIntrospectionResults缓存。
	 * @since 4.2
	 * @see ReflectionUtils#clearCache()
	 * @see AnnotationUtils#clearCache()
	 * @see ResolvableType#clearCache()
	 * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
	 */
	protected void resetCommonCaches() {
		// 清除一些缓存
		ReflectionUtils.clearCache();
		AnnotationUtils.clearCache();
		ResolvableType.clearCache();
		CachedIntrospectionResults.clearClassLoader(getClassLoader());
	}


	/**
	 * Register a shutdown hook {@linkplain Thread#getName() named}
	 * {@code SpringContextShutdownHook} with the JVM runtime, closing this
	 * context on JVM shutdown unless it has already been closed at that time.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * @see Runtime#addShutdownHook
	 * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
	 * @see #close()
	 * @see #doClose()
	 */
	@Override
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// No shutdown hook registered yet.
			this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
				@Override
				public void run() {
					synchronized (startupShutdownMonitor) {
						doClose();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * Callback for destruction of this instance, originally attached
	 * to a {@code DisposableBean} implementation (not anymore in 5.0).
	 * <p>The {@link #close()} method is the native way to shut down
	 * an ApplicationContext, which this method simply delegates to.
	 * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
	 */
	@Deprecated
	public void destroy() {
		close();
	}

	/**
	 * Close this application context, destroying all beans in its bean factory.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	@Override
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// If we registered a JVM shutdown hook, we don't need it anymore now:
			// We've already explicitly closed the context.
			if (this.shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				}
				catch (IllegalStateException ex) {
					// ignore - VM is already shutting down
				}
			}
		}
	}

	/**
	 * Actually performs context closing: publishes a ContextClosedEvent and
	 * destroys the singletons in the bean factory of this application context.
	 * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see #destroyBeans()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	@SuppressWarnings("deprecation")
	protected void doClose() {
		// Check whether an actual close attempt is necessary...
		if (this.active.get() && this.closed.compareAndSet(false, true)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this);
			}

			if (!NativeDetector.inNativeImage()) {
				LiveBeansView.unregisterApplicationContext(this);
			}

			try {
				// Publish shutdown event.
				// 发布上下文关闭事件
				publishEvent(new ContextClosedEvent(this));
			}
			catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// Stop all Lifecycle beans, to avoid delays during individual destruction.
			// 停止所有生命周期bean,以避免在单个销毁期间造成延迟。
			if (this.lifecycleProcessor != null) {
				try {
					this.lifecycleProcessor.onClose();
				}
				catch (Throwable ex) {
					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
				}
			}

			// Destroy all cached singletons in the context's BeanFactory.
			// 销毁上下文的BeanFactory中所有缓存的单例
			destroyBeans();

			// Close the state of this context itself.
			// 关闭此上下文本身的状态
			closeBeanFactory();

			// Let subclasses do some final clean-up if they wish...
			// 由子类实现, 让子类完成相关资源的清理
			onClose();

			// Reset local application listeners to pre-refresh state.
			// 将本地应用程序侦听器重置为预刷新状态
			if (this.earlyApplicationListeners != null) {
				this.applicationListeners.clear();
				this.applicationListeners.addAll(this.earlyApplicationListeners);
			}

			// Switch to inactive.
			// 切换为非活动状态
			this.active.set(false);
		}
	}

	/**
	 * Template method for destroying all beans that this context manages.
	 * The default implementation destroy all cached singletons in this context,
	 * invoking {@code DisposableBean.destroy()} and/or the specified
	 * "destroy-method".
	 * <p>Can be overridden to add context-specific bean destruction steps
	 * right before or right after standard singleton destruction,
	 * while the context's BeanFactory is still active.
	 * 用于销毁此上下文管理的所有bean的模板方法。默认实现通过调用DisposableBean.destroy()和/或指定的“destroy方法”销毁此上下文中所有缓存的单例。
	 * 可以重写以在标准单例销毁之前或之后添加上下文特定的bean销毁步骤，而上下文的BeanFactory仍然处于活动状态
	 * @see #getBeanFactory()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
	 */
	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}

	/**
	 * Template method which can be overridden to add context-specific shutdown work.
	 * The default implementation is empty.
	 * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
	 * this context's BeanFactory has been closed. If custom shutdown logic
	 * needs to execute while the BeanFactory is still active, override
	 * the {@link #destroyBeans()} method instead.
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	@Override
	public boolean isActive() {
		return this.active.get();
	}

	/**
	 * Assert that this context's BeanFactory is currently active,
	 * throwing an {@link IllegalStateException} if it isn't.
	 * <p>Invoked by all {@link BeanFactory} delegation methods that depend
	 * on an active context, i.e. in particular all bean accessor methods.
	 * <p>The default implementation checks the {@link #isActive() 'active'} status
	 * of this context overall. May be overridden for more specific checks, or for a
	 * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
	 */
	protected void assertBeanFactoryActive() {
		if (!this.active.get()) {
			if (this.closed.get()) {
				throw new IllegalStateException(getDisplayName() + " has been closed already");
			}
			else {
				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, requiredType);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType, args);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name, allowFactoryBeanInit);
	}

	@Override
	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	@Override
	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * Return the internal bean factory of the parent context if it implements
	 * ConfigurableApplicationContext; else, return the parent context itself.
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	@Nullable
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	@Override
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * Return the internal MessageSource used by the context.
	 * @return the internal MessageSource (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	@Nullable
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext ?
				((AbstractApplicationContext) getParent()).messageSource : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() {
		getLifecycleProcessor().start();
		publishEvent(new ContextStartedEvent(this));
	}

	@Override
	public void stop() {
		getLifecycleProcessor().stop();
		publishEvent(new ContextStoppedEvent(this));
	}

	@Override
	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Subclasses must implement this method to perform the actual configuration load.
	 * The method is invoked by {@link #refresh()} before any other initialization work.
	 * <p>A subclass will either create a new bean factory and hold a reference to it,
	 * or return a single BeanFactory instance that it holds. In the latter case, it will
	 * usually throw an IllegalStateException if refreshing the context more than once.
	 * @throws BeansException if initialization of the bean factory failed
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * Subclasses must implement this method to release their internal bean factory.
	 * This method gets invoked by {@link #close()} after all other shutdown work.
	 * <p>Should never throw an exception but rather log shutdown failures.
	 */
	protected abstract void closeBeanFactory();

	/**
	 * Subclasses must return their internal bean factory here. They should implement the
	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
	 * <p>Note: Subclasses should check whether the context is still active before
	 * returning the internal bean factory. The internal factory should generally be
	 * considered unavailable once the context has been closed.
	 * @return this application context's internal bean factory (never {@code null})
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 * (usually if {@link #refresh()} has never been called) or if the context has been
	 * closed already
	 * @see #refreshBeanFactory()
	 * @see #closeBeanFactory()
	 */
	@Override
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * Return information about this context.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(", started on ").append(new Date(getStartupDate()));
		ApplicationContext parent = getParent();
		if (parent != null) {
			sb.append(", parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}

}
