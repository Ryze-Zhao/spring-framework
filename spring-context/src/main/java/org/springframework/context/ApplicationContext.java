/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * Central interface to provide configuration for an application.
 * This is read-only while the application is running, but may be
 * reloaded if the implementation supports this.
 *
 * <p>An ApplicationContext provides:
 * <ul>
 * <li>Bean factory methods for accessing application components.
 * Inherited from {@link org.springframework.beans.factory.ListableBeanFactory}.
 * <li>The ability to load file resources in a generic fashion.
 * Inherited from the {@link org.springframework.core.io.ResourceLoader} interface.
 * <li>The ability to publish events to registered listeners.
 * Inherited from the {@link ApplicationEventPublisher} interface.
 * <li>The ability to resolve messages, supporting internationalization.
 * Inherited from the {@link MessageSource} interface.
 * <li>Inheritance from a parent context. Definitions in a descendant context
 * will always take priority. This means, for example, that a single parent
 * context can be used by an entire web application, while each servlet has
 * its own child context that is independent of that of any other servlet.
 * </ul>
 *
 * <p>In addition to standard {@link org.springframework.beans.factory.BeanFactory}
 * lifecycle capabilities, ApplicationContext implementations detect and invoke
 * {@link ApplicationContextAware} beans as well as {@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware} and {@link MessageSourceAware} beans.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ConfigurableApplicationContext
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.ResourceLoader
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * Return the unique id of this application context.
	 * 返回此应用程序上下文的唯一id。
	 * @return the unique id of the context, or {@code null} if none 	上下文的唯一id，如果没有，则为null
	 */
	@Nullable
	String getId();

	/**
	 * Return a name for the deployed application that this context belongs to.
	 * 返回此上下文所属的已部署应用程序的名称。
	 * @return a name for the deployed application, or the empty String by default	已部署应用程序的名称，默认情况下为空字符串
	 */
	String getApplicationName();

	/**
	 * Return a friendly name for this context.
	 * 已部署应用程序的名称，
	 * @return a display name for this context (never {@code null})	默认情况下为空字符串
	 */
	String getDisplayName();

	/**
	 * Return the timestamp when this context was first loaded.
	 * 返回首次加载此上下文时的时间戳。
	 * @return the timestamp (ms) when this context was first loaded	首次加载此上下文时的时间戳（ms
	 */
	long getStartupDate();

	/**
	 * Return the parent context, or {@code null} if there is no parent and this is the root of the context hierarchy.
	 * 返回父上下文，如果没有父上下文且这是上下文层次结构的根，则返回null。
	 * @return the parent context, or {@code null} if there is no parent	父上下文，如果没有父上下文，则为null
	 */
	@Nullable
	ApplicationContext getParent();

	/**
	 * Expose AutowireCapableBeanFactory functionality for this context.
	 * <p>This is not typically used by application code, except for the purpose of
	 * initializing bean instances that live outside of the application context,
	 * applying the Spring bean lifecycle (fully or partly) to them.
	 * <p>Alternatively, the internal BeanFactory exposed by the
	 * {@link ConfigurableApplicationContext} interface offers access to the
	 * {@link AutowireCapableBeanFactory} interface too. The present method mainly
	 * serves as a convenient, specific facility on the ApplicationContext interface.
	 * <p><b>NOTE: As of 4.2, this method will consistently throw IllegalStateException
	 * after the application context has been closed.</b> In current Spring Framework
	 * versions, only refreshable application contexts behave that way; as of 4.2,
	 * all application context implementations will be required to comply.
	 *
	 * 为此上下文公开AutowireCapableBeanFactory功能。
	 * 这通常不被应用程序代码使用，除非是为了初始化位于应用程序上下文之外的bean实例，对它们应用springbean生命周期（全部或部分）。
	 * 或者，ConfigurableApplicationContext接口公开的内部BeanFactory也提供对AutowireCapableBeanFactory接口的访问。
	 * 本方法主要用作ApplicationContext接口上的一种方便、特定的工具。
	 * 注意：从4.2开始，此方法将在应用程序上下文关闭后始终抛出IllegalStateException。
	 * 在当前的Spring框架版本中，只有可刷新的应用程序上下文才会这样做；从4.2开始，所有应用程序上下文实现都需要遵守。
	 *
	 * @return the AutowireCapableBeanFactory for this context	此上下文的AutowireCapableBeanFactory
	 * @throws IllegalStateException if the context does not support the
	 * {@link AutowireCapableBeanFactory} interface, or does not hold an
	 * autowire-capable bean factory yet (e.g. if {@code refresh()} has
	 * never been called), or if the context has been closed already	如果上下文不支持AutowireCapableBeanFactory接口，或者尚未拥有支持autowire的bean工厂（例如，如果从未调用过refresh（）），或者如果上下文已经关闭
	 * @see ConfigurableApplicationContext#refresh()
	 * @see ConfigurableApplicationContext#getBeanFactory()
	 */
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
