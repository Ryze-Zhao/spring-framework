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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

/**
 * Abstract base class for property resource configurers that resolve placeholders
 * in bean definition property values. Implementations <em>pull</em> values from a
 * properties file or other {@linkplain org.springframework.core.env.PropertySource
 * property source} into bean definitions.
 *
 * <p>The default placeholder syntax follows the Ant / Log4J / JSP EL style:
 *
 * <pre class="code">${...}</pre>
 *
 * Example XML bean definition:
 *
 * <pre class="code">
 * &lt;bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource"&gt;
 *   &lt;property name="driverClassName" value="${driver}" /&gt;
 *   &lt;property name="url" value="jdbc:${dbname}" /&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * Example properties file:
 *
 * <pre class="code">
 * driver=com.mysql.jdbc.Driver
 * dbname=mysql:mydb</pre>
 *
 * Annotated bean definitions may take advantage of property replacement using
 * the {@link org.springframework.beans.factory.annotation.Value @Value} annotation:
 *
 * <pre class="code">@Value("${person.age}")</pre>
 *
 * Implementations check simple property values, lists, maps, props, and bean names
 * in bean references. Furthermore, placeholder values can also cross-reference
 * other placeholders, like:
 *
 * <pre class="code">
 * rootPath=myrootdir
 * subPath=${rootPath}/subdir</pre>
 *
 * In contrast to {@link PropertyOverrideConfigurer}, subclasses of this type allow
 * filling in of explicit placeholders in bean definitions.
 *
 * <p>If a configurer cannot resolve a placeholder, a {@link BeanDefinitionStoreException}
 * will be thrown. If you want to check against multiple properties files, specify multiple
 * resources via the {@link #setLocations locations} property. You can also define multiple
 * configurers, each with its <em>own</em> placeholder syntax. Use {@link
 * #ignoreUnresolvablePlaceholders} to intentionally suppress throwing an exception if a
 * placeholder cannot be resolved.
 *
 * <p>Default property values can be defined globally for each configurer instance
 * via the {@link #setProperties properties} property, or on a property-by-property basis
 * using the value separator which is {@code ":"} by default and customizable via
 * {@link #setValueSeparator(String)}.
 *
 * <p>Example XML property with default value:
 *
 * <pre class="code">
 *   &lt;property name="url" value="jdbc:${dbname:defaultdb}" /&gt;
 * </pre>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertyPlaceholderConfigurer
 * @see org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 */
public abstract class PlaceholderConfigurerSupport extends PropertyResourceConfigurer
		implements BeanNameAware, BeanFactoryAware {

	/**
	 * Default placeholder prefix: {@value}.
	 * 默认使用的占位符前缀
	 */
	public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

	/**
	 * Default placeholder suffix: {@value}.
	 * 默认使用的占位符后缀
	 */
	public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

	/**
	 * Default value separator: {@value}.
	 * 默认使用的值分隔符
	 */
	public static final String DEFAULT_VALUE_SEPARATOR = ":";


	/**
	 * Defaults to {@value #DEFAULT_PLACEHOLDER_PREFIX}.
	 * 实例成员所用的占位符前缀
	 */
	protected String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

	/**
	 * Defaults to {@value #DEFAULT_PLACEHOLDER_SUFFIX}.
	 * 实例成员所用的占位符后缀
	 */
	protected String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

	/**
	 * Defaults to {@value #DEFAULT_VALUE_SEPARATOR}.
	 * 实例成员所用的值分隔符
	 */
	@Nullable
	protected String valueSeparator = DEFAULT_VALUE_SEPARATOR;

	/**
	 * 是否要对值做 trim.
	 */
	protected boolean trimValues = false;

	/**
	 * 遇到占位符对应属性值为""或者null时的替代填充值.
	 */
	@Nullable
	protected String nullValue;

	/**
	 * 能解析的占位符是否抛出异常， false 表示抛出异常， true 表示不抛出异常.
	 */
	protected boolean ignoreUnresolvablePlaceholders = false;

	/**
	 * 对应 BeanNameAware 接口方法setBeanName()用于记录当前bean的名称.
	 */
	@Nullable
	private String beanName;

	/**
	 * 对应 BeanFactoryAware 接口方法setBeanFactory()用于记录当前bean所在容器，也就是需要处理的bean定义所在的容器.
	 */
	@Nullable
	private BeanFactory beanFactory;


	/**
	 * Set the prefix that a placeholder string starts with.
	 * The default is {@value #DEFAULT_PLACEHOLDER_PREFIX}.
	 */
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
	 * Set the suffix that a placeholder string ends with.
	 * The default is {@value #DEFAULT_PLACEHOLDER_SUFFIX}.
	 */
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
	 * Specify the separating character between the placeholder variable
	 * and the associated default value, or {@code null} if no such
	 * special character should be processed as a value separator.
	 * The default is {@value #DEFAULT_VALUE_SEPARATOR}.
	 */
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.valueSeparator = valueSeparator;
	}

	/**
	 * Specify whether to trim resolved values before applying them,
	 * removing superfluous whitespace from the beginning and end.
	 * <p>Default is {@code false}.
	 * @since 4.3
	 */
	public void setTrimValues(boolean trimValues) {
		this.trimValues = trimValues;
	}

	/**
	 * Set a value that should be treated as {@code null} when resolved
	 * as a placeholder value: e.g. "" (empty String) or "null".
	 * <p>Note that this will only apply to full property values,
	 * not to parts of concatenated values.
	 * <p>By default, no such null value is defined. This means that
	 * there is no way to express {@code null} as a property value
	 * unless you explicitly map a corresponding value here.
	 */
	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	/**
	 * Set whether to ignore unresolvable placeholders.
	 * <p>Default is "false": An exception will be thrown if a placeholder fails
	 * to resolve. Switch this flag to "true" in order to preserve the placeholder
	 * String as-is in such a case, leaving it up to other placeholder configurers
	 * to resolve it.
	 */
	public void setIgnoreUnresolvablePlaceholders(boolean ignoreUnresolvablePlaceholders) {
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}

	/**
	 * Only necessary to check that we're not parsing our own bean definition,
	 * to avoid failing on unresolvable placeholders in properties file locations.
	 * The latter case can happen with placeholders for system properties in
	 * resource locations.
	 * @see #setLocations
	 * @see org.springframework.core.io.ResourceEditor
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Only necessary to check that we're not parsing our own bean definition,
	 * to avoid failing on unresolvable placeholders in properties file locations.
	 * The latter case can happen with placeholders for system properties in
	 * resource locations.
	 * @see #setLocations
	 * @see org.springframework.core.io.ResourceEditor
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	/**
	 * .
	 * 该方法是 PlaceholderConfigurerSupport 对容器中所有 bean 定义进行处理的核心逻辑方法，
	 * 该方法留给具体实现子类使用，当然具体实现子类也可以重写该方法或者不使用该方法。
	 *
	 * Spring 提供的 PlaceholderConfigurerSupport 具体实现子类 PropertyPlaceholderConfigurer直接使用了该方法。
	 *
	 * 参数 beanFactoryToProcess ： 要处理的bean定义所属的容器
	 * 参数 valueResolver : 属性值解析器
	 */
	protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
			StringValueResolver valueResolver) {

		//  BeanDefinitionVisitor 是一个专门为 PropertyPlaceholderConfigurer 设计的 bean 定义访问器，
		//  <Spring分析点36-1>主要是遍历 bean 定义中的属性值和构造函数参数值字符串，使用 valueResolver 解析相应值字符串中的占位符，结合给定的属性源，将占位符其替换为相应的属性值
		BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);

		// 获取容器中所有的 bean 名称，这样可以访问到容器中所有的 bean 定义
		String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
		for (String curName : beanNames) {
			// Check that we're not parsing our own bean definition,to avoid failing on unresolvable placeholders in properties file locations.
			// <Spring分析点36-2>
			// this.beanName 记录了当前对象，也就是当前 PlaceholderConfigurerSupport 对象的bean 名称，
			// 这里的 for 循环会使用 visitor 处理处理当前 PlaceholderConfigurerSupport 对象之外的所有 bean 定义
			// !(curName.equals(this.beanName)   当前实例 PlaceholderConfigurerSupport 不在解析范围内
			// beanFactoryToProcess.equals(this.beanFactory))  同一个 Spring 容器
			if (!(curName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
				BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(curName);
				try {
					visitor.visitBeanDefinition(bd);
				}
				catch (Exception ex) {
					throw new BeanDefinitionStoreException(bd.getResourceDescription(), curName, ex.getMessage(), ex);
				}
			}
		}

		// New in Spring 2.5: resolve placeholders in alias target names and aliases as well.
		// <Spring分析点36-3> bean 的别名，以及别名的目标bean名称中也可能使用占位符，这里也做处理
		beanFactoryToProcess.resolveAliases(valueResolver);

		// New in Spring 3.0: resolve placeholders in embedded values such as annotation attributes.
		// <Spring分析点36-4> 将这里所使用的值解析器 valueResolver 添加到容器，用于处理类似注解属性中的嵌入值。
		beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
	}

}
