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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

/**
 * Interface for resolving properties against any underlying source.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Environment
 * @see PropertySourcesPropertyResolver
 */
public interface PropertyResolver {

	/**
	 * Return whether the given property key is available for resolution, i.e. if the value for the given key is not {@code null}.
	 * 是否包含某个属性
	 *
	 */
	boolean containsProperty(String key);

	/**
	 * Return the property value associated with the given key,or {@code null} if the key cannot be resolved.
	 * 获取属性值 如果找不到key返回null
	 * @param key the property name to resolve	要解析的属性名称
	 * @see #getProperty(String, String)
	 * @see #getProperty(String, Class)
	 * @see #getRequiredProperty(String)
	 */
	@Nullable
	String getProperty(String key);

	/**
	 * Return the property value associated with the given key, or {@code defaultValue} if the key cannot be resolved.
	 * 获取属性值，如果找不到key，则返回defaultValue。
	 * @param key the property name to resolve	要解析的属性名称
	 * @param defaultValue the default value to return if no value is found	未找到值时返回的默认值
	 * @see #getRequiredProperty(String)
	 * @see #getProperty(String, Class)
	 */
	String getProperty(String key, String defaultValue);

	/**
	 * Return the property value associated with the given key,or {@code null} if the key cannot be resolved.
	 * 获取指定类型的属性值，如果找不到key，则返回null。
	 * @param key the property name to resolve	要解析的属性名称
	 * @param targetType the expected type of the property value	属性值的预期类型
	 * @see #getRequiredProperty(String, Class)
	 */
	@Nullable
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * Return the property value associated with the given key, or {@code defaultValue} if the key cannot be resolved.
	 * 获取指定类型的属性值，如果找不到key，则返回defaultValue。
	 * @param key the property name to resolve	要解析的属性名称
	 * @param targetType the expected type of the property value	属性值的预期类型
	 * @param defaultValue the default value to return if no value is found	未找到值时返回的默认值
	 * @see #getRequiredProperty(String, Class)
	 */
	<T> T getProperty(String key, Class<T> targetType, T defaultValue);

	/**
	 * Return the property value associated with the given key (never {@code null}).
	 * 获取属性值，找不到抛出异常IllegalStateException
	 * @throws IllegalStateException if the key cannot be resolved	如果无法解析key
	 * @see #getRequiredProperty(String, Class)
	 */
	String getRequiredProperty(String key) throws IllegalStateException;

	/**
	 * Return the property value associated with the given key, converted to the given targetType (never {@code null}).
	 * 获取指定类型的属性值，找不到抛出异常IllegalStateException
	 * @throws IllegalStateException if the given key cannot be resolved	如果无法解析key
	 */
	<T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value are ignored and passed through unchanged.
	 * 替换文本中的占位符（${key}）到属性值，找不到不解析
	 * 解析给定文本中的${…}占位符，将其替换为由getProperty解析的相应属性值。不带默认值的不可解析占位符将被忽略并原封不动地传递。
	 * @param text the String to resolve	要解析的字符串
	 * @return the resolved String (never {@code null})		解析的字符串（从不为空）
	 * @throws IllegalArgumentException if given text is {@code null}	如果给定文本为空
	 * @see #resolveRequiredPlaceholders
	 */
	String resolvePlaceholders(String text);

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value will cause an IllegalArgumentException to be thrown.
	 * 替换文本中的占位符（${key}）到属性值，找不到抛出异常IllegalArgumentException
	 * 解析给定文本中的${…}占位符，将其替换为由getProperty解析的相应属性值。没有默认值的不可解析占位符将导致引发IllegalArgumentException。
	 * @return the resolved String (never {@code null})	解析的字符串（从不为空）
	 * @throws IllegalArgumentException if given text is {@code null} or if any placeholders are unresolvable
	 * 									如果给定文本为空或任何占位符无法解析
	 */
	String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
