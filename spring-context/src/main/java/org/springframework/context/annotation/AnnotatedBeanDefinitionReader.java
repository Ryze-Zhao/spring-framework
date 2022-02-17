/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient adapter for programmatic registration of bean classes.
 *
 * <p>This is an alternative to {@link ClassPathBeanDefinitionScanner}, applying
 * the same resolution of annotations but for explicitly registered classes only.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.0
 * @see AnnotationConfigApplicationContext#register
 */
public class AnnotatedBeanDefinitionReader {

	private final BeanDefinitionRegistry registry;

	/**
	 * .
	 * 名字生成器
	 *  可以自定义怎么生成，默认是简单类名首字母小写
	 */
	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	/**
	 * .
	 * 范围注解解析器
	 *  解析出范围，是单例，还是原型
	 */
	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	/**
	 * .
	 * 条件评估器
	 */
	private ConditionEvaluator conditionEvaluator;


	/**
	 * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry.
	 * <p>If the registry is {@link EnvironmentCapable}, e.g. is an {@code ApplicationContext},
	 * the {@link Environment} will be inherited, otherwise a new
	 * {@link StandardEnvironment} will be created and used.
	 * @param registry the {@code BeanFactory} to load bean definitions into,
	 * in the form of a {@code BeanDefinitionRegistry}
	 * @see #AnnotatedBeanDefinitionReader(BeanDefinitionRegistry, Environment)
	 * @see #setEnvironment(Environment)
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		// get0rCreateEnvironment主要就是配置文件和属性/profiles和properties
		this(registry, getOrCreateEnvironment(registry));
	}

	/**
	 * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry,
	 * using the given {@link Environment}.
	 * @param registry the {@code BeanFactory} to load bean definitions into,
	 * in the form of a {@code BeanDefinitionRegistry}
	 * @param environment the {@code Environment} to use when evaluating bean definition
	 * profiles.
	 * @since 3.1
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(environment, "Environment must not be null");
		this.registry = registry;
		// 用于@Conditional注解的实现
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
		// 重点:注册注解配置处理器
		// 当用注解的方式启动项目时，将处理这些注解的基础设施类放到DefaultListableBeanFactory中，
		// 因为无论如何，Spring终究需要某些基础类，用于xml中的配置类，或者我们的注解类，比如@Configuration、@ComponentScan、@ComponentScans、@Import、@ImportResource、@Bean等
		// registerAnnotationConfigProcessors方法作用就是如此，将Spring几个基础类封装成BeanDefinition放到容器当中
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}


	/**
	 * Get the BeanDefinitionRegistry that this reader operates on.
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Set the {@code Environment} to use when evaluating whether
	 * {@link Conditional @Conditional}-annotated component classes should be registered.
	 * <p>The default is a {@link StandardEnvironment}.
	 * @see #registerBean(Class, String, Class...)
	 */
	public void setEnvironment(Environment environment) {
		this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
	}

	/**
	 * Set the {@code BeanNameGenerator} to use for detected bean classes.
	 * <p>The default is a {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator =
				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
	}

	/**
	 * Set the {@code ScopeMetadataResolver} to use for registered component classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}


	/**
	 * Register one or more component classes to be processed.
	 * <p>Calls to {@code register} are idempotent; adding the same
	 * component class more than once has no additional effect.
	 * 注册一个或多个要处理的组件类。 对注册的调用是幂等的；多次添加同一组件类不会产生额外的效果。
	 *
	 * @param componentClasses one or more component classes,
	 * e.g. {@link Configuration @Configuration} classes
	 */
	public void register(Class<?>... componentClasses) {
		for (Class<?> componentClass : componentClasses) {
			registerBean(componentClass);
		}
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param beanClass the class of the bean
	 */
	public void registerBean(Class<?> beanClass) {
		doRegisterBean(beanClass, null, null, null, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param beanClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @since 5.2
	 */
	public void registerBean(Class<?> beanClass, @Nullable String name) {
		doRegisterBean(beanClass, name, null, null, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param beanClass the class of the bean
	 * @param qualifiers specific qualifier annotations to consider,
	 * in addition to qualifiers at the bean class level
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers) {
		doRegisterBean(beanClass, null, qualifiers, null, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param beanClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @param qualifiers specific qualifier annotations to consider,
	 * in addition to qualifiers at the bean class level
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> beanClass, @Nullable String name,
			Class<? extends Annotation>... qualifiers) {

		doRegisterBean(beanClass, name, qualifiers, null, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations, using the given supplier for obtaining a new
	 * instance (possibly declared as a lambda expression or method reference).
	 * @param beanClass the class of the bean
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable Supplier<T> supplier) {
		doRegisterBean(beanClass, null, null, supplier, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations, using the given supplier for obtaining a new
	 * instance (possibly declared as a lambda expression or method reference).
	 * @param beanClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier) {
		doRegisterBean(beanClass, name, null, supplier, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param beanClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @since 5.2
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier,
			BeanDefinitionCustomizer... customizers) {

		doRegisterBean(beanClass, name, null, supplier, customizers);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from class-declared annotations.
	 * 对传入的 Spring 配置类解析，其实也是解析为一个 `BeanDefinition` 然后注册到容器中
	 *
	 *
	 *
	 * @param beanClass the class of the bean   bean的类
	 * @param name an explicit name for the bean bean的显式名称
	 * @param qualifiers specific qualifier annotations to consider, if any,in addition to qualifiers at the bean class level
	 *                   除了bean类级别中的限定符之外，要考虑的特定限定符注释（如果有的话）
	 * @param supplier a callback for creating an instance of the bean(may be {@code null})
	 *                 用于创建bean实例的回调（可能为空）
	 * @param customizers one or more callbacks for customizing the factory's {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 *                    用于自定义工厂BeanDefinition的一个或多个回调，例如设置lazy init或primary标志
	 * @since 5.0
	 */
	private <T> void doRegisterBean(Class<T> beanClass, @Nullable String name,
			@Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier,
			@Nullable BeanDefinitionCustomizer[] customizers) {

		// 根据指定的注解BeanDefinition类 AnnotatedGenericBeanDefinition，创建Spring容器中对注解Bean的封装的数据结构
		// 通过传入的Bean类的Class对象 实例化一个AnnotatedGenericBeanDefinition对象，里面会初始化一个 AnnotationMetadata（包括Bean类的全部注解）
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
		// 通过AnnotationMetadata 接口判断是否需要跳过（具体看里面实现吧）
		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
			return;
		}

		// 设置 InstanceSupplier 属性（设置bean创建的回调）
		abd.setInstanceSupplier(supplier);
		// 解析作用域元数据：解析 AnnotatedBeanDefinition 的作用域，若@Scope("prototype")，则Bean为原型类型；若@Scope("singleton")，则Bean为单例类型(默认）
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
		// 设置作用域：为 AnnotatedBeanDefinition 设置作用域
		abd.setScope(scopeMetadata.getScopeName());
		// 获取bean名字：为 AnnotatedBeanDefinition 生成Bean名称
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));


		// 处理 AnnotatedBeanDefinition 中的通用注解，主要是@Lazy、@DependsOn、 @Description、@Primary、@Role等等注解
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
		// 如果在向容器注册 AnnotatedBeanDefinition 时，使用了额外的限定符注解，则解析限定符注解。
		// 主要是配置的关于autowiring自动依赖注入装配的限定条件，即@Qualifier注解，Spring自动依赖注入装配默认是按类型装配，如果使用@Qualifier则按名称

		// byName 和 qualifiers 这个变量是Annotation类型的数组，里面存不仅仅是Qualifier注解
		// 理论上里面存的是一切注解，所以下面Spring去循环了这个数组，然后依次判断了注解当中是否包含了@Primary，是否包含了@Lazy
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				// 如果配置了@Primary注解，设置该Bean为autowiring自动依赖注入装配时的首选
				if (Primary.class == qualifier) {
					abd.setPrimary(true);
				}
				// 如果配置了@Lazy注解，则设置该Bean为延迟初始化，如果没有配置，则该Bean为预实例化
				else if (Lazy.class == qualifier) {
					abd.setLazyInit(true);
				}
				// 如果使用了除@Primary和@Lazy以外的其他注解，则为该Bean添加一个autowiring自动依赖注入装配限定符，
				// 该Bean在进行autowiring自动依赖注入装配时，根据名称装配限定符指定的Bean
				else {
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}

		// 对bean做自定义操作注册：通常用在applicationContext创建后，手动向容器中使用lambda表达式的方式注册bean
		// 比如：applicationContext.registerBean(UserService.class, () -> new UserService());
		if (customizers != null) {
			for (BeanDefinitionCustomizer customizer : customizers) {
				// 自定义bean添加到BeanDefinition
				customizer.customize(abd);
			}
		}
		// 创建一个指定Bean名称的 BeanDefinitionHolder ，封装 AnnotatedBeanDefinition 数据
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
		// 根据通过 AnnotationMetadata 类中配置的作用域 ScopeMetadata ，创建相应的代理对象
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		// 向IoC容器注册 BeanDefinitionHolder
		// BeanDefinitionReaderUtils.registerBeanDefinition 内部通过
		// DefaultListableBeanFactory.registerBeanDefinition(String beanName, BeanDefinition beanDefinition)按名称将BeanDefinition信息注册到容器中，
		// 实际上 DefaultListableBeanFactory 内部维护一个Map<String, BeanDefinition>类型变量beanDefinitionMap，用于保存注BeanDefinition信息（beanName 和 BeanDefinition映射）
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}


	/**
	 * Get the Environment from the given registry if possible, otherwise return a new
	 * StandardEnvironment.
	 *
	 * 如果可能，从给定BeanDefinitionRegistry获取环境，否则返回新的StandardEnvironment
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
