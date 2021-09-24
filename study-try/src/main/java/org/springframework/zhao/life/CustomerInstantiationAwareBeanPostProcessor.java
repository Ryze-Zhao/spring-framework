package org.springframework.zhao.life;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;


@Component
public class CustomerInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
	/**
	 * 实例化之前的处理
	 * @author : HeHaoZhao
	 */
	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		System.out.println("执行InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation：：beanName=" + beanName);
		return null;
	}

	/**
	 * 实例化之后的处理
	 * @author : HeHaoZhao
	 */
	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		System.out.println("执行InstantiationAwareBeanPostProcessor#postProcessAfterInstantiation：：beanName=" + beanName);
		return true;
	}

	/**
	 * 修改属性值
	 * @author : HeHaoZhao
	 */
	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		System.out.println("执行InstantiationAwareBeanPostProcessor#postProcessProperties：：beanName=" + beanName);
		return pvs;
	}

	/**
	 * 初始化之前的处理
	 * @author : HeHaoZhao
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("执行InstantiationAwareBeanPostProcessor#postProcessBeforeInitialization：：beanName=" + beanName);
		return bean;
	}

	/**
	 * 初始化之后的处理
	 * @author : HeHaoZhao
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("执行InstantiationAwareBeanPostProcessor#postProcessAfterInitialization：：beanName=" + beanName);
		return bean;
	}
}