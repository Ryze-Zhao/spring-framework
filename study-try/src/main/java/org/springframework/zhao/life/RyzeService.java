package org.springframework.zhao.life;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.context.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;
import org.springframework.util.StringValueResolver;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;


public class RyzeService implements
		BeanNameAware, BeanClassLoaderAware, BeanFactoryAware, EnvironmentAware, EmbeddedValueResolverAware,
		ResourceLoaderAware, ApplicationEventPublisherAware, MessageSourceAware, ApplicationContextAware,
		InitializingBean, DisposableBean {

	private String ryzeName;

	public RyzeService() {
		System.out.println("执行无参构造函数：：Initializing");
	}

	public String getRyzeName() {
		return ryzeName;
	}

	public void setRyzeName(String ryzeName) {
		this.ryzeName = ryzeName;
		System.out.println("执行set函数：：" + ryzeName);
	}

	@Override
	public void setBeanName(@NonNull String beanName) {
		System.out.println("执行setBeanName：：" + beanName);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		System.out.println("执行setBeanClassLoader：：" + classLoader.getClass().getName());
	}

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
		System.out.println("执行setBeanFactory：：" + beanFactory);
	}

	@Override
	public void setEnvironment(@NonNull Environment environment) {
		System.out.println("执行setEnvironment：：" + environment);
	}

	@Override
	public void setEmbeddedValueResolver(@NonNull StringValueResolver resolver) {
		System.out.println("执行setEmbeddedValueResolver：：");
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Resource resource = resourceLoader.getResource("classpath:spring/life/bean/SpringConfig.xml");
		System.out.println("执行setResourceLoader:: " + resource.getFilename());
	}


	@Override
	public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
		System.out.println("执行setApplicationEventPublisher");
	}


	@Override
	public void setMessageSource(@NonNull MessageSource messageSource) {
		System.out.println("执行setMessageSource");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		System.out.println("执行setApplicationContext:: Bean Definition Names=" + Arrays.toString(applicationContext.getBeanDefinitionNames()));
	}

	@PostConstruct
	public void initPostConstruct() {
		System.out.println("执行@PostConstruct注解的方法");
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("执行InitializingBean接口的实现方法afterPropertiesSet");
	}


	/**
	 * 通过<bean>的init-method属性指定的初始化方法
	 * @author : HeHaoZhao
	 */
	public void initMethod() throws Exception {
		System.out.println("执行init-method配置的方法");
	}


	@PreDestroy
	public void preDestroy() {
		System.out.println("执行@preDestroy注解的方法");
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("执行DisposableBean接口的实现方法destroy");
	}

	/**
	 * 通过<bean>的destroy-method属性指定的销毁方法
	 * @author : HeHaoZhao
	 */
	public void destroyMethod() throws Exception {
		System.out.println("执行destroy-method配置的方法");
	}
}