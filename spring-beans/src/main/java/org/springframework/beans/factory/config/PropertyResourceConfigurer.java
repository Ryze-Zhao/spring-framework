/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.util.ObjectUtils;

/**
 * Allows for configuration of individual bean property values from a property resource,
 * i.e. a properties file. Useful for custom config files targeted at system
 * administrators that override bean properties configured in the application context.
 *
 * <p>Two concrete implementations are provided in the distribution:
 * <ul>
 * <li>{@link PropertyOverrideConfigurer} for "beanName.property=value" style overriding
 * (<i>pushing</i> values from a properties file into bean definitions)
 * <li>{@link PropertyPlaceholderConfigurer} for replacing "${...}" placeholders
 * (<i>pulling</i> values from a properties file into bean definitions)
 * </ul>
 *
 * <p>Property values can be converted after reading them in, through overriding
 * the {@link #convertPropertyValue} method. For example, encrypted values
 * can be detected and decrypted accordingly before processing them.
 *
 * @author Juergen Hoeller
 * @since 02.10.2003
 * @see PropertyOverrideConfigurer
 * @see PropertyPlaceholderConfigurer
 */
public abstract class PropertyResourceConfigurer extends PropertiesLoaderSupport
		implements BeanFactoryPostProcessor, PriorityOrdered {

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * Set the order value of this object for sorting purposes.
	 * @see PriorityOrdered
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * {@linkplain #mergeProperties Merge}, {@linkplain #convertProperties convert} and
	 * {@linkplain #processProperties process} properties against the given bean factory.
	 * 针对给定的 `BeanFactory` 合并、转换和处理属性。 指定人： 接口 `BeanFactoryPostProcessor` 中的后处理BeanFactory
	 * 
	 *
	 * 参数： beanFactory– `ApplicationContext` 使用的 `BeanFactory`  `ApplicationContext` 使用的 `BeanFactory`
	 * @throws BeanInitializationException if any properties cannot be loaded	如果无法加载任何属性
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
			// <Spring分析点35-1> 返回合并的 Properties 实例，合并本地属性和外部指定的属性文件资源中的属性
			Properties mergedProps = mergeProperties();

			// Convert the merged properties, if necessary.
			// <Spring分析点35-2> 将属性的值做转换(仅在必要的时候做)
			convertProperties(mergedProps);

			// Let the subclass process the properties.
			// <Spring分析点35-3> 具体子类处理，对容器中的每个bean定义进行处理，也就是替换每个bean定义中的属性中的占位符
			processProperties(beanFactory, mergedProps);
		}
		catch (IOException ex) {
			throw new BeanInitializationException("Could not load properties", ex);
		}
	}

	/**
	 * Convert the given merged properties, converting property values if necessary. The result will then be processed.
	 * <p>The default implementation will invoke {@link #convertPropertyValue} for each property value, replacing the original with the converted value.
	 * 转换给定的合并属性，必要时转换属性值。然后将对结果进行处理。
	 * 默认实现将为每个属性值调用convertPropertyValue，用转换后的值替换原始值
	 * @param props the Properties to convert
	 * @see #processProperties
	 */
	protected void convertProperties(Properties props) {
		Enumeration<?> propertyNames = props.propertyNames();
		while (propertyNames.hasMoreElements()) {
			String propertyName = (String) propertyNames.nextElement();
			String propertyValue = props.getProperty(propertyName);
			// 对指定名称的属性的属性值进行必要的转换
			String convertedValue = convertProperty(propertyName, propertyValue);
			if (!ObjectUtils.nullSafeEquals(propertyValue, convertedValue)) {
				props.setProperty(propertyName, convertedValue);
			}
		}
	}

	/**
	 * Convert the given property from the properties source to the value which should be applied.
	 * <p>The default implementation calls {@link #convertPropertyValue(String)}.
	 * 将属性源中的给定属性转换为应应用的值。 默认实现调用convertPropertyValue（字符串）。
	 *
	 * @param propertyName the name of the property that the value is defined for   为其定义值的属性的名称
	 * @param propertyValue the original value from the properties source       属性源中的原始值
	 * @return the converted value, to be used for processing       转换后的值，用于处理
	 * @see #convertPropertyValue(String)
	 */
	protected String convertProperty(String propertyName, String propertyValue) {
		// 对属性值的必要转换，这是一个默认实现，不做任何转换直接返回原值，实现类可以覆盖该方法
		return convertPropertyValue(propertyValue);
	}

	/**
	 * Convert the given property value from the properties source to the value which should be applied.
	 * <p>The default implementation simply returns the original value.
	 * Can be overridden in subclasses, for example to detect encrypted values and decrypt them accordingly.
	 * 将属性源中的给定属性值转换为应应用的值。
	 * 默认实现只返回原始值。
	 * 可以在子类中重写，例如检测加密值并相应地解密它们。
	 * @param originalValue the original value from the properties source (properties file or local "properties")   来自属性源（属性文件或本地“属性”）的原始值
	 * @return the converted value, to be used for processing   转换后的值，用于处理
	 * @see #setProperties
	 * @see #setLocations
	 * @see #setLocation
	 * @see #convertProperty(String, String)
	 */
	protected String convertPropertyValue(String originalValue) {
		return originalValue;
	}


	/**
	 * Apply the given Properties to the given BeanFactory.
	 * 将给定的属性应用于给定的BeanFactory。
	 * @param beanFactory the BeanFactory used by the application context	 `ApplicationContext` 使用的beanFactory
	 * @param props the Properties to apply		要应用的属性
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	protected abstract void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException;

}
