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

package org.springframework.context;

import java.util.Locale;

import org.springframework.lang.Nullable;

/**
 * Strategy interface for resolving messages, with support for the parameterization
 * and internationalization of such messages.
 *
 * <p>Spring provides two out-of-the-box implementations for production:
 * <ul>
 * <li>{@link org.springframework.context.support.ResourceBundleMessageSource}: built
 * on top of the standard {@link java.util.ResourceBundle}, sharing its limitations.
 * <li>{@link org.springframework.context.support.ReloadableResourceBundleMessageSource}:
 * highly configurable, in particular with respect to reloading message definitions.
 * </ul>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.support.ResourceBundleMessageSource
 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
 */
public interface MessageSource {

	/**
	 * Try to resolve the message. Return default message if no message was found.
	 * 尝试解析该消息。如果未找到消息，则返回默认消息。
	 *
	 * @param code the message code to look up, e.g. 'calculator.noRateSet'.
	 * MessageSource users are encouraged to base message names on qualified class
	 * or package names, avoiding potential conflicts and ensuring maximum clarity.
	 *             要查找的消息代码，例如“calculator.noRateSet”。鼓励MessageSource用户将消息名称基于限定的类或包名称，避免潜在冲突并确保最大程度的清晰性。
	 * @param args an array of arguments that will be filled in for params within
	 * the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
	 * or {@code null} if none
	 *             将为消息中的参数填充的参数数组（参数看起来像消息中的“{0}”、“1，date}”、“2，time}”），如果没有，则为null
	 * @param defaultMessage a default message to return if the lookup fails	查找失败时返回的默认消息
	 * @param locale the locale in which to do the lookup	执行查找的区域设置
	 * @return the resolved message if the lookup was successful, otherwise
	 * the default message passed as a parameter (which may be {@code null})
	 * 				如果查找成功，则返回已解析的消息，否则将默认消息作为参数传递（可能为null）
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	@Nullable
	String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale);

	/**
	 * Try to resolve the message. Treat as an error if the message can't be found.
	 * 尝试解析该消息。如果找不到消息，则视为错误。
	 *
	 * @param code the message code to look up, e.g. 'calculator.noRateSet'.
	 * MessageSource users are encouraged to base message names on qualified class
	 * or package names, avoiding potential conflicts and ensuring maximum clarity.
	 *             要查找的消息代码，例如“calculator.noRateSet”。鼓励MessageSource用户将消息名称基于限定的类或包名称，避免潜在冲突并确保最大程度的清晰性。
	 * @param args an array of arguments that will be filled in for params within
	 * the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
	 * or {@code null} if none
	 *             将为消息中的参数填充的参数数组（参数看起来像消息中的“{0}”、“1，date}”、“2，time}”），如果没有，则为null
	 * @param locale the locale in which to do the lookup	执行查找的区域设置
	 * @return the resolved message (never {@code null})	已解析的消息（从不为空）
	 * @throws NoSuchMessageException if no corresponding message was found		如果未找到相应的消息
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException;

	/**
	 * Try to resolve the message using all the attributes contained within the
	 * {@code MessageSourceResolvable} argument that was passed in.
	 * <p>NOTE: We must throw a {@code NoSuchMessageException} on this method
	 * since at the time of calling this method we aren't able to determine if the
	 * {@code defaultMessage} property of the resolvable is {@code null} or not.
	 * 尝试使用传入的MessageSourceResolvable参数中包含的所有属性解析消息。
	 * 注意：我们必须对此方法抛出NoSuchMessageException，因为在调用此方法时，我们无法确定resolvable的defaultMessage属性是否为null。
	 *
	 * @param resolvable the value object storing attributes required to resolve a message
	 * (may include a default message)	存储解析消息所需属性的值对象（可能包括默认消息）
	 * @param locale the locale in which to do the lookup	执行查找的区域设置
	 * @return the resolved message (never {@code null} since even a
	 * {@code MessageSourceResolvable}-provided default message needs to be non-null)
	 * 			 已解析的消息（从不为null，因为即使MessageSourceResolvable提供的默认消息也需要为非null）
	 * @throws NoSuchMessageException if no corresponding message was found
	 * (and no default message was provided by the {@code MessageSourceResolvable})
	 * 			如果未找到相应的消息（并且MessageSourceResolvable未提供默认消息）
	 * @see MessageSourceResolvable#getCodes()
	 * @see MessageSourceResolvable#getArguments()
	 * @see MessageSourceResolvable#getDefaultMessage()
	 * @see java.text.MessageFormat
	 */
	String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException;

}
