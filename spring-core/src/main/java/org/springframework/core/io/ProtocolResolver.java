/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.io;

import org.springframework.lang.Nullable;

/**
 * A resolution strategy for protocol-specific resource handles.
 *
 * <p>Used as an SPI for {@link DefaultResourceLoader}, allowing for
 * custom protocols to be handled without subclassing the loader
 * implementation (or application context implementation).
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultResourceLoader#addProtocolResolver
 */
@FunctionalInterface
public interface ProtocolResolver {

	/**
	 * Resolve the given location against the given resource loader if this implementation's protocol matches.
	 * 如果此实现的协议匹配，则根据给定的resourceLoader(资源加载程序)解析给定的位置。
	 * <p>
	 * 在介绍 Resource 时，提到如果要实现自定义 Resource，我们只需要继承 AbstractResource 即可；
	 * 然后要实现自定义读取Resource，我们只需要继承 DefaultResourceLoader 即可，
	 * 但是有了 ProtocolResolver 后，我们要实现自定义读取Resource，也可以不需要直接继承 DefaultResourceLoader，改为实现 ProtocolResolver 接口也可以实现自定义的 ResourceLoader。
	 *
	 * @param location       the user-specified resource location 用户指定的资源位置
	 * @param resourceLoader the associated resource loader 关联的资源加载器
	 * @return a corresponding {@code Resource} handle if the given location 如果给定位置与此解析器的协议匹配，则为相应的资源句柄，否则为null
	 * matches this resolver's protocol, or {@code null} otherwise
	 */
	@Nullable
	Resource resolve(String location, ResourceLoader resourceLoader);

}
