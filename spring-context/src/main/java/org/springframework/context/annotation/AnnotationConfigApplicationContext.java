/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Standalone application context, accepting <em>component classes</em> as input &mdash;
 * in particular {@link Configuration @Configuration}-annotated classes, but also plain
 * {@link org.springframework.stereotype.Component @Component} types and JSR-330 compliant
 * classes using {@code javax.inject} annotations.
 *
 * <p>Allows for registering classes one by one using {@link #register(Class...)}
 * as well as for classpath scanning using {@link #scan(String...)}.
 *
 * <p>In case of multiple {@code @Configuration} classes, {@link Bean @Bean} methods
 * defined in later classes will override those defined in earlier classes. This can
 * be leveraged to deliberately override certain bean definitions via an extra
 * {@code @Configuration} class.
 *
 * <p>See {@link Configuration @Configuration}'s javadoc for usage examples.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see #register
 * @see #scan
 * @see AnnotatedBeanDefinitionReader
 * @see ClassPathBeanDefinitionScanner
 * @see org.springframework.context.support.GenericXmlApplicationContext
 */
public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {


	/**
	 * .
	 * AnnotatedBeanDefinitionReader 作用是读取加了注解的Bean
	 * 这个类在构造方法中实例化
	 */
	private final AnnotatedBeanDefinitionReader reader;


	/**
	 * .
	 * ClassPathBeanDefinitionScanner 作用是扫描加了注解的Bean
	 * 这个类在构造方法中实例化
	 */
	private final ClassPathBeanDefinitionScanner scanner;


	/**
	 * Create a new AnnotationConfigApplicationContext that needs to be populated
	 * through {@link #register} calls and then manually {@linkplain #refresh refreshed}.
	 *
	 * 初始化一个bean的读取器:AnnotatedBeanDefinitionReader 和扫描器:ClassPathBeanDefinitionScanner
	 * 默认构造函数，如果直接调用这个默认构造方法，需要在稍后通过调用其register()
	 * 去注册配置类（JavaConfig），并调用refresh()方法刷新容器，触发容器对注解Bean的载入、解析和注册过程
	 *
	 *  注意：在初始化 AnnotatedBeanDefinitionReader 和 ClassPathBeanDefinitionScanner 时需要传递的是 BeanDefinitionRegistry 用来存储解析好的 BeanDefinition信息
	 *  而 AnnotationConfigApplicationContext 实现了 GenericApplicationContext,而 GenericApplicationContext 实现了 BeanDefinitionRegistry 接口

	 */
	public AnnotationConfigApplicationContext() {
		// 分步记录有关ApplicationStartup期间发生的特定阶段或操作的指标
		StartupStep createAnnotatedBeanDefReader = this.getApplicationStartup().start("spring.context.annotated-bean-reader.create");
		// 创建一个读取注解的BeanDefinitionReader，AnnotationConfigApplicationContext 间接实现了 BeanDefinitionRegistry 接口
		// 实例化注解bean的解析器, 并会对beanFactory进行一些初始化配置
		this.reader = new AnnotatedBeanDefinitionReader(this);
		// 分步记录有关ApplicationStartup 结束
		createAnnotatedBeanDefReader.end();

		/*
		 *  ClassPathBeanDefinitionScanner: 可以用来扫描包或者类，继而转换成 BeanDefinition
		 *      1.当初始化 AnnotationConfigApplicationContext 时传入的是配置类的Class信息时，在后面根据注解信息获取到包信息并扫描时使用的并不是这个scanner对象,
		 *      而是spring内部实例化的一个ClassPathBeanDefinitionScanner {@link ComponentScanAnnotationParser#parse(org.springframework.core.annotation.AnnotationAttributes, java.lang.String)}
		 *      2.当初始化 AnnotationConfigApplicationContext 时传入的是配置类的包路径信息时，在{@link this#scan(String...)}方法中对包路径进行扫描时使用的是该scanner对象
		 *  总之: 两种方式对包的扫描工作都是在{@link ClassPathBeanDefinitionScanner#doScan(String...)}中进行的
		 *
		 *  注意：
		 *      可以用来扫描包或者类，继而转换成BeanDefinition，但实际上Spring扫描包并不是使用scanner这个对象，而是Spring内部自行new ClassPathBeanDefinitionScanner扫描
		 *      这里的scanner仅仅是为了程序员能够在外部调用 AnnotationConfigApplicationContext对象scan方法
		 */
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * Create a new AnnotationConfigApplicationContext with the given DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 */
	public AnnotationConfigApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, deriving bean definitions from the given component classes and automatically refreshing the context.
	 * 创建新的AnnotationConfigApplicationContext，从给定的组件类派生BeanDefinition并自动刷新上下文。
	 *
	 * 这个构造函数需要传入一个被javaconfig注解了的配置类，然后会把这个被注解了javaconfig的类通过注解读取器读取后继而解析
	 * @param componentClasses one or more component classes &mdash; for example, 一个或多个组件类-例如@Configuration类
	 * {@link Configuration @Configuration} classes
	 */
	public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
		// 这里由于他有父类，故而会先调用父类的构造方法，然后才会调用自己的构造方法
		// 在自己构造方法中初始一个读取器:AnnotatedBeanDefinitionReader 和扫描器:ClassPathBeanDefinitionScanner
		this();
		/*
		 * 注册bean配置类
		 * 程序执行到这步之前, Spring内部的BeanDefinition(BeanFactory后处理器)已经注册到beanFactory中;
		 * 该步骤只是将手动提供的annotatedClasses(配置类)也注册到beanFactory中;
		 * 其他的注解bean还没有被注册, 整个注册过程刚刚开始
		 * 其他的注解bean是在激活后处理器中的方法时, 对配置类进行解析, 解析到@ComponentScan注解中的包路径后才进行解析注册
		 */
		register(componentClasses);
		// 刷新上下文（核心方法）
		refresh();
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, scanning for components in the given packages, registering bean definitions for those components,and automatically refreshing the context.
	 * 创建新的AnnotationConfigApplicationContext，扫描给定包中的组件，注册这些组件的BeanDefinition，并自动刷新上下文
	 *
	 * @param basePackages the packages to scan for component classes 要扫描组件类的包
	 */
	public AnnotationConfigApplicationContext(String... basePackages) {
		// 这里由于他有父类，故而会先调用父类的构造方法，然后才会调用自己的构造方法
		// 在自己构造方法中初始一个读取器:AnnotatedBeanDefinitionReader 和扫描器:ClassPathBeanDefinitionScanner
		this();
        // 扫描给定包中的组件
		scan(basePackages);
		// 刷新上下文（核心方法）
		refresh();
	}


	/**
	 * Propagate the given custom {@code Environment} to the underlying
	 * {@link AnnotatedBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
	}

	/**
	 * Provide a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}, if any.
	 * <p>Default is {@link AnnotationBeanNameGenerator}.
	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 * @see AnnotationBeanNameGenerator
	 * @see FullyQualifiedAnnotationBeanNameGenerator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.reader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
		getBeanFactory().registerSingleton(
				AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
	}

	/**
	 * Set the {@link ScopeMetadataResolver} to use for registered component classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.reader.setScopeMetadataResolver(scopeMetadataResolver);
		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
	}


	//---------------------------------------------------------------------
	// Implementation of AnnotationConfigRegistry
	//---------------------------------------------------------------------

	/**
	 * Register one or more component classes to be processed.
	 * <p>Note that {@link #refresh()} must be called in order for the context to fully process the new classes.
	 * 注册一个或多个要处理的组件类。
	 * 请注意，必须调用refresh（），ApplicationContext 才能完全处理新类。
	 * @param componentClasses one or more component classes &mdash; for example,
	 * {@link Configuration @Configuration} classes 在接口注释ConfigRegistry中注册，一个或多个组件类-例如@Configuration类
	 * @see #scan(String...)
	 * @see #refresh()
	 */
	@Override
	public void register(Class<?>... componentClasses) {
		Assert.notEmpty(componentClasses, "At least one component class must be specified");
		StartupStep registerComponentClass = this.getApplicationStartup().start("spring.context.component-classes.register")
				.tag("classes", () -> Arrays.toString(componentClasses));
		this.reader.register(componentClasses);
		registerComponentClass.end();
	}

	/**
	 * Perform a scan within the specified base packages.
	 * <p>Note that {@link #refresh()} must be called in order for the context
	 * to fully process the new classes.
	 * @param basePackages the packages to scan for component classes
	 * @see #register(Class...)
	 * @see #refresh()
	 */
	@Override
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		StartupStep scanPackages = this.getApplicationStartup().start("spring.context.base-packages.scan")
				.tag("packages", () -> Arrays.toString(basePackages));
		this.scanner.scan(basePackages);
		scanPackages.end();
	}


	//---------------------------------------------------------------------
	// Adapt superclass registerBean calls to AnnotatedBeanDefinitionReader
	//---------------------------------------------------------------------

	@Override
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
			@Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		this.reader.registerBean(beanClass, beanName, supplier, customizers);
	}

}
