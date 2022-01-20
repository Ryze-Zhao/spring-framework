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

package org.springframework.context.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ApplicationStartupAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import org.springframework.core.NativeDetector;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 *
 * <p>Registered by default when using {@code <context:annotation-config/>} or
 * {@code <context:component-scan/>}. Otherwise, may be declared manually as
 * with any other {@link BeanFactoryPostProcessor}.
 *
 * <p>This post processor is priority-ordered as it is important that any
 * {@link Bean @Bean} methods declared in {@code @Configuration} classes have
 * their corresponding bean definitions registered before any other
 * {@code BeanFactoryPostProcessor} executes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.0
 */
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
		PriorityOrdered, ResourceLoaderAware, ApplicationStartupAware, BeanClassLoaderAware, EnvironmentAware {

	/**
	 * A {@code BeanNameGenerator} using fully qualified class names as default bean names.
	 * <p>This default for configuration-level import purposes may be overridden through
	 * {@link #setBeanNameGenerator}. Note that the default for component scanning purposes
	 * is a plain {@link AnnotationBeanNameGenerator#INSTANCE}, unless overridden through
	 * {@link #setBeanNameGenerator} with a unified user-level bean name generator.
	 * @since 5.2
	 * @see #setBeanNameGenerator
	 */
	public static final AnnotationBeanNameGenerator IMPORT_BEAN_NAME_GENERATOR =
			FullyQualifiedAnnotationBeanNameGenerator.INSTANCE;

	private static final String IMPORT_REGISTRY_BEAN_NAME =
			ConfigurationClassPostProcessor.class.getName() + ".importRegistry";


	private final Log logger = LogFactory.getLog(getClass());

	private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	@Nullable
	private Environment environment;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	private boolean setMetadataReaderFactoryCalled = false;

	private final Set<Integer> registriesPostProcessed = new HashSet<>();

	private final Set<Integer> factoriesPostProcessed = new HashSet<>();

	@Nullable
	private ConfigurationClassBeanDefinitionReader reader;

	private boolean localBeanNameGeneratorSet = false;

	/* Using short class names as default bean names by default. */
	private BeanNameGenerator componentScanBeanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	/* Using fully qualified class names as default bean names by default. */
	private BeanNameGenerator importBeanNameGenerator = IMPORT_BEAN_NAME_GENERATOR;

	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
	}

	/**
	 * Set the {@link SourceExtractor} to use for generated bean definitions
	 * that correspond to {@link Bean} factory methods.
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new PassThroughSourceExtractor());
	}

	/**
	 * Set the {@link ProblemReporter} to use.
	 * <p>Used to register any problems detected with {@link Configuration} or {@link Bean}
	 * declarations. For instance, an @Bean method marked as {@code final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * Set the {@link MetadataReaderFactory} to use.
	 * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
	 * {@linkplain #setBeanClassLoader bean class loader}.
	 */
	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.setMetadataReaderFactoryCalled = true;
	}

	/**
	 * Set the {@link BeanNameGenerator} to be used when triggering component scanning
	 * from {@link Configuration} classes and when registering {@link Import}'ed
	 * configuration classes. The default is a standard {@link AnnotationBeanNameGenerator}
	 * for scanned components (compatible with the default in {@link ClassPathBeanDefinitionScanner})
	 * and a variant thereof for imported configuration classes (using unique fully-qualified
	 * class names instead of standard component overriding).
	 * <p>Note that this strategy does <em>not</em> apply to {@link Bean} methods.
	 * <p>This setter is typically only appropriate when configuring the post-processor as a
	 * standalone bean definition in XML, e.g. not using the dedicated {@code AnnotationConfig*}
	 * application contexts or the {@code <context:annotation-config>} element. Any bean name
	 * generator specified against the application context will take precedence over any set here.
	 * @since 3.1.1
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		Assert.notNull(beanNameGenerator, "BeanNameGenerator must not be null");
		this.localBeanNameGeneratorSet = true;
		this.componentScanBeanNameGenerator = beanNameGenerator;
		this.importBeanNameGenerator = beanNameGenerator;
	}

	@Override
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
		}
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		this.applicationStartup = applicationStartup;
	}

	/**
	 * Derive further bean definitions from the configuration classes in the registry.
	 */
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		// 生成唯一标识，用于重复处理验证
		int registryId = System.identityHashCode(registry);
		if (this.registriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
		}
		if (this.factoriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + registry);
		}
		this.registriesPostProcessed.add(registryId);
		// 解析Java类并配置Bean
		processConfigBeanDefinitions(registry);
	}

	/**
	 * Prepare the Configuration classes for servicing bean requests at runtime
	 * by replacing them with CGLIB-enhanced subclasses.
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		int factoryId = System.identityHashCode(beanFactory);
		if (this.factoriesPostProcessed.contains(factoryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + beanFactory);
		}
		this.factoriesPostProcessed.add(factoryId);
		// 下面的if语句不会进入
		// 因为在执行BeanFactoryPostProcessor时，会先执行BeanDefinitionRegistryPostProcessor#postProcessorBeanDefinitionRegistry()方法
		// 而在执行postProcessorBeanDefinitionRegistry()方法时，都会调用processConfigBeanDefinitions()方法
		//
		// 而这里与postProcessorBeanDefinitionRegistry()方法的执行逻辑是一样的，为了避免重复执行，所以在执行方法之前会先生成一个id，将id放入到一个set当中，
		// 每次执行之前先判断id是否存在，所以在此处，永远不会进入到if语句中
		if (!this.registriesPostProcessed.contains(factoryId)) {
			// BeanDefinitionRegistryPostProcessor hook apparently not supported...
			// Simply call processConfigurationClasses lazily at this point then.
			// 该方法在这里不会被执行到
			processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
		}
		// 对加了@Configuration注解的配置类进行Cglib代理
		enhanceConfigurationClasses(beanFactory);
		// beanFactory 添加一个BeanPostProcessor后置处理器
		beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
	}

	/**
	 * Build and validate a configuration model based on the registry of {@link Configuration} classes.
	 */
	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		// 初始化 BeanDefinitionHolder 集合
		List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
		// 获取 BeanFactory 中的 beanDefinitionNames（所有已经注册的BeanName），
		// 其实就是之前 AnnotationConfigUtils#registerAnnotationConfigProcessors注册的，再加上我们的配置类
		/*
		 * (1).org.springframework.context.annotation.internalConfigurationAnnotationProcessor   -->只有这一个实现了BeanDefinitionRegistryPostProcessor接口
		 * (2).org.springframework.context.annotation.internalAutowiredAnnotationProcessor
		 * (3).org.springframework.context.annotation.internalCommonAnnotationProcessor
		 * (4).org.springframework.context.annotation.internalPersistenceAnnotationProcessor    -->这个没有被注册,JPA环境下注册
		 * (5).org.springframework.context.event.internalEventListenerProcessor
		 * (6).org.springframework.context.event.internalEventListenerFactory
		 * (7).zhaoConfig
		 * 注：5.1前仍有一个org.springframework.context.annotation.internalRequiredAnnotationProcessor（现已废弃）
		 */
		String[] candidateNames = registry.getBeanDefinitionNames();
		// <Spring分析点40-1> 遍历 所有已经注册的BeanName,, 将加了@Configuration, @Component, @ComponentScan, @Import, @ImportResource注解的配置类 添加到configCandidates集合
		for (String beanName : candidateNames) {
			// 通过BeanName，得到BeanDefinition实例
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);

			/*
			 * Spring内部对配置类分为两种：
			 *      Full模式（@Configuration(proxyBeanMethods = true)）与Lite模式（@Configuration(proxyBeanMethods = false)）
			 * 因此获取ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE有值，代表当前处理的 BeanDefinition 已经被解析，如果Debug模式下，输出日志
			 */
			if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
				}
			}
			// 如果当前的bean是JavaBean配置类（含有@Configuration注解的类），则加入到集合configCandidates中
			//   checkConfigurationClassCandidate()会判断 BeanDefinition 是否是一个配置类,并为BeanDefinition设置属性为lite或者full，方便后面用
			//     如果加了@Configuration，并且proxyBeanMethods属性设置为true，那么对应的BeanDefinition为full，否则为lite
			//     如果加了@Bean、@Component(及派生的注解)、@ComponentScan、@Import、@ImportResource，那么对应的BeanDefinition为lite
			else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}

		// Return immediately if no @Configuration classes were found
		/*
		 * 如果没有@Configuration自定义注解类,直接退出
		 * 下面只对添加@Configuration,@Component, @ComponentScan, @Import, @ImportResource注解的类进行处理
		 */
		if (configCandidates.isEmpty()) {
			return;
		}

		// Sort by previously determined @Order value, if applicable
		// <Spring分析点40-2> 对configCandidates 进行 排序,按照@Order 进行排序
		configCandidates.sort((bd1, bd2) -> {
			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
			return Integer.compare(i1, i2);
		});

		// Detect any custom bean name generation strategy supplied through the enclosing application context
		// 初始化一个ConfigurationClassParser解析器，可以解析@Congiguration配置类
		SingletonBeanRegistry sbr = null;
		// 判断 BeanDefinitionRegistry 是不是 SingletonBeanRegistry子类，若是会将 registry 强转为 SingletonBeanRegistry
		// 通常初始化时都是DefaultListableBeanFactory，而他是SingletonBeanRegistry 的子类，所以通常这里为true
		if (registry instanceof SingletonBeanRegistry) {
			sbr = (SingletonBeanRegistry) registry;
			//是否有自定义的
			if (!this.localBeanNameGeneratorSet) {
				// BeanName生成器，因为后面会扫描出所有加入到Spring容器中class类，然后把这些class解析成BeanDefinition类，
				// 此时需要利用BeanNameGenerator为这些BeanDefinition生成beanName

				// 看看有无自定义的 BeanName生成器，若有，那就直接用
				BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
				// 如果有则直接用，否则用Spring默认的
				if (generator != null) {
					this.componentScanBeanNameGenerator = generator;
					this.importBeanNameGenerator = generator;
				}
			}
		}

		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}

		//  <Spring分析点40-3> 循环解析每个配置类
		// Parse each @Configuration class
		// 初始并实例化 一个ConfigurationClassParser解析器，可以解析@Congiguration配置类
		ConfigurationClassParser parser = new ConfigurationClassParser(
				this.metadataReaderFactory, this.problemReporter, this.environment,
				this.resourceLoader, this.componentScanBeanNameGenerator, registry);

		// 实例化2个Set
		// 将configCandidates(List)集合中的BeanDefinitionHolder放入candidates(Set)集合中进行去重,因为可能有多个配置类重复了
		// alreadyParsed 用于判断是否处理过
		Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
		Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());

		do {
			StartupStep processConfig = this.applicationStartup.start("spring.context.config-classes.parse");
			// candidates: 需要解析的配置类集合
			//  对添加注解(@Configuration, @Component, @ComponentScan, @Import, @ImportResource等)的类进行解析，该步骤会递归完成了对所有加注解的bean的解析工作
			//
			// 解析Java配置类，在此处会解析配置类上的注解(@ComponentScan扫描出的类，@Import注册的类，以及@Bean方法定义的类)
			// 注意：这一步只会将加了@Configuration注解以及通过@ComponentScan注解扫描的类 加入到BeanDefinitionMap中
			// 通过其他注解(例如@Import、@Bean)的方式，在parse()方法这一步并不会将其解析为BeanDefinition和不会放入到BeanDefinitionMap中，
			// 而是先解析成ConfigurationClass类，真正将这些其他注解的类放入到BeanDefinitionMap中是在下面的this.reader.loadBeanDefinitions()方法中实现的
			parser.parse(candidates);
			// 主要校验配置类不能使用final修饰符（CGLIB代理是生成一个子类，因此原先的类不能使用final修饰）
			parser.validate();

			// 获取所有的配置类
			Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
			// /移除所有已经解析过的配置类
			configClasses.removeAll(alreadyParsed);

			// Read the model and create bean definitions based on its content
			// 读取模型并根据其内容创建BeanDefinitionReader
			if (this.reader == null) {
				this.reader = new ConfigurationClassBeanDefinitionReader(
						registry, this.sourceExtractor, this.resourceLoader, this.environment,
						this.importBeanNameGenerator, parser.getImportRegistry());
			}
			// 将上一步parser解析出的ConfigurationClass类加载成BeanDefinition
			// 实际上经过上一步的parse()后，解析出来的bean已经放入到BeanDefinition中了，但是由于这些Bean又可能会引入新的bean，
			// 例如：实现了ImportBeanDefinitionRegistrar或者ImportSelector接口的bean，或者bean中存在被@Bean注解的方法
			// 因此需要执行一次loadBeanDefinition()，这样就会执行ImportBeanDefinitionRegistrar、ImportSelector接口的方法、@Bean注释的方法
			this.reader.loadBeanDefinitions(configClasses);
			alreadyParsed.addAll(configClasses);
			processConfig.tag("classCount", () -> String.valueOf(configClasses.size())).end();
			// 清空已处理的配置类
			candidates.clear();

			// 再次获取容器中bean定义数量  如果大于 之前获取的bean定义数量，则说明有新的bean注册到容器中，需要再次解析
			// 这里判断registry.getBeanDefinitionCount() > candidateNames.length的目的是为了知道reader.loadBeanDefinitions(configClasses)这一步有没有向BeanDefinitionMap中添加新的BeanDefinition
			// 实际上就是看配置类(假如ZhaoConfig类会向BeanDefinitionMap中添加bean)，如果有，registry.getBeanDefinitionCount()就会大于candidateNames.length
			// 这样就需要再次遍历新加入的BeanDefinition，并判断这些bean是否已经被解析过了，如果未解析，需要重新进行解析
			// 我们假如的ZhaoConfig类向容器中添加的bean，实际上在parser.parse()这一步已经全部被解析了
			if (registry.getBeanDefinitionCount() > candidateNames.length) {
				String[] newCandidateNames = registry.getBeanDefinitionNames();
				Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
				Set<String> alreadyParsedClasses = new HashSet<>();
				for (ConfigurationClass configurationClass : alreadyParsed) {
					alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
				}
				// 如果有未解析的类，则将其添加到candidates中，这样candidates不为空，就会进入到下一次的while的循环中
				for (String candidateName : newCandidateNames) {
					// 处理新的
					if (!oldCandidateNames.contains(candidateName)) {
						BeanDefinition bd = registry.getBeanDefinition(candidateName);
						// 新注册的bean如果也是@Configuration配置类,则添加到数据，等待解析
						if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
								!alreadyParsedClasses.contains(bd.getBeanClassName())) {
							// 符合配置类要求就添加到candidates
							candidates.add(new BeanDefinitionHolder(bd, candidateName));
						}
					}
				}
				// 更新候选名字
				candidateNames = newCandidateNames;
			}
		}
		while (!candidates.isEmpty());

		// Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
		// 把importStack注册为单例，为了支持ImportAware接口扩展，说明已经处理Import注解过了
		if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
			sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
		}

		// 有些bean定义的元数据是通过URL加载字节码文件解析来的，为了避免每次都去IO操作，会有元数据的缓存，现在处理完了就要把缓存清除了
		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			// Clear cache in externally provided MetadataReaderFactory; this is a no-op
			// for a shared cache since it'll be cleared by the ApplicationContext.
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
	}

	/**
	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
	 * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
	 * Candidate status is determined by BeanDefinition attribute metadata.
	 * @see ConfigurationClassEnhancer
	 */
	public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		StartupStep enhanceConfigClasses = this.applicationStartup.start("spring.context.config-classes.enhance");
		Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			Object configClassAttr = beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);
			AnnotationMetadata annotationMetadata = null;
			MethodMetadata methodMetadata = null;
			if (beanDef instanceof AnnotatedBeanDefinition) {
				AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDef;
				annotationMetadata = annotatedBeanDefinition.getMetadata();
				methodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
			}
			if ((configClassAttr != null || methodMetadata != null) && beanDef instanceof AbstractBeanDefinition) {
				// Configuration class (full or lite) or a configuration-derived @Bean method
				// -> eagerly resolve bean class at this point, unless it's a 'lite' configuration
				// or component class without @Bean methods.
				AbstractBeanDefinition abd = (AbstractBeanDefinition) beanDef;
				if (!abd.hasBeanClass()) {
					boolean liteConfigurationCandidateWithoutBeanMethods =
							(ConfigurationClassUtils.CONFIGURATION_CLASS_LITE.equals(configClassAttr) &&
								annotationMetadata != null && !ConfigurationClassUtils.hasBeanMethods(annotationMetadata));
					if (!liteConfigurationCandidateWithoutBeanMethods) {
						try {
							abd.resolveBeanClass(this.beanClassLoader);
						}
						catch (Throwable ex) {
							throw new IllegalStateException(
									"Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
						}
					}
				}
			}
			if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {
				if (!(beanDef instanceof AbstractBeanDefinition)) {
					throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
							beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
				}
				else if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {
					logger.info("Cannot enhance @Configuration bean definition '" + beanName +
							"' since its singleton instance has been created too early. The typical cause " +
							"is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
							"return type: Consider declaring such methods as 'static'.");
				}
				configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
			}
		}
		if (configBeanDefs.isEmpty() || NativeDetector.inNativeImage()) {
			// nothing to enhance -> return immediately
			enhanceConfigClasses.end();
			return;
		}

		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
			AbstractBeanDefinition beanDef = entry.getValue();
			// If a @Configuration class gets proxied, always proxy the target class
			beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			// Set enhanced subclass of the user-specified bean class
			Class<?> configClass = beanDef.getBeanClass();
			// 调用ConfigurationClassEnhancer.enhance()方法创建增强类
			Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
			if (configClass != enhancedClass) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +
							"enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
				}
				beanDef.setBeanClass(enhancedClass);
			}
		}
		enhanceConfigClasses.tag("classCount", () -> String.valueOf(configBeanDefs.keySet().size())).end();
	}


	private static class ImportAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

		private final BeanFactory beanFactory;

		public ImportAwareBeanPostProcessor(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public PropertyValues postProcessProperties(@Nullable PropertyValues pvs, Object bean, String beanName) {
			// Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's
			// postProcessProperties method attempts to autowire other configuration beans.
			// 为被CGLIB增强时实现了EnhancedConfiguration接口的代理类，设置beanFactory属性
			if (bean instanceof EnhancedConfiguration) {
				((EnhancedConfiguration) bean).setBeanFactory(this.beanFactory);
			}
			return pvs;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			if (bean instanceof ImportAware) {
				ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
				AnnotationMetadata importingClass = ir.getImportingClassFor(ClassUtils.getUserClass(bean).getName());
				if (importingClass != null) {
					((ImportAware) bean).setImportMetadata(importingClass);
				}
			}
			return bean;
		}
	}

}
