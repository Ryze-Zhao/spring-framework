package org.springframework.zhao.life;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;


public class CustomerBeanPostProcessor implements BeanPostProcessor {
	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		System.out.println("执行BeanPostProcessor#postProcessBeforeInitialization：：beanName=" + beanName);
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		System.out.println("执行BeanPostProcessor#postProcessAfterInitialization：：beanName=" + beanName);
		return bean;
	}
}