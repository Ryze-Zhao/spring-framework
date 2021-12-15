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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;
import org.springframework.core.NativeDetector;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {


	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		/*
		 * 判断是否满足下面条件的
		 * 1. config.isOptimize(): 代理是否应执行积极的优化 (默认:false)
		 *     用来控制通过CGLIB创建的代理是否使用激进的优化策略，除非完全了解AOP代理如何处理优化，否则不推荐用户使用这个设置，目前这个属性仅用于CGLIB 代理，对于JDK动态代理无效
		 *
		 * 2. config.isProxyTargetClass(): 是否直接代理目标类以及任何接口 (默认:false)，也就是用户确定要使用CGLIB（但如果Spring判断到目标类更适合使用JDK代理的话，依然会使用JDK代理）
		 *
		 * 3. hasNoUserSuppliedProxyInterfaces(config): 目标类是否有实现的接口,没有的话返回true (JDK动态代理基于接口,所以只有目标类存在接口才会使用JDK动态代理)
		 */
		if (!NativeDetector.inNativeImage() &&
				(config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config))) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// 如果目标类是一个接口 或者 是 java.lang.reflect.Proxy 的子类 则还是使用 JDK 动态代理，创建一个 JdkDynamicAopProxy 对象，
			// 传入 AdvisedSupport 配置管理器，并返回
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			// 使用 CGLIB 动态代理，创建一个  ObjenesisCglibAopProxy 对象，传入 AdvisedSupport 配置管理器，并返回
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			// 使用 JDK 动态代理，创建一个 JdkDynamicAopProxy 对象，传入 AdvisedSupport 配置管理器，并返回
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
