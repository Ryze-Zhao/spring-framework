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

package org.springframework.aop.aspectj.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;

	@Nullable
	private volatile List<String> aspectBeanNames;

	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();


	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 * 去容器中获取到所有的切面信息保存到缓存中
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 */
	public List<Advisor> buildAspectJAdvisors() {
		// 先尝试从缓存中获取切面名称列表
		List<String> aspectNames = this.aspectBeanNames;
		// 缓存字段aspectNames没有值 注意实例化第一个单实例Bean的时候就会触发解析切面
		if (aspectNames == null) {
			// 双重检查
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					// advisors集合存储容器中所有的切面类中定义的增强/通知Advice (注意是增强/通知, 没有切入点)
					// advisors 用于保存所有解析出来的Advisors集合对象
					List<Advisor> advisors = new ArrayList<>();
					// 用于保存切面的名称的集合
					aspectNames = new ArrayList<>();
					/*
					 * 获取所有Object类型的beanName
					 * AOP功能中在这里传入的是Object对象，代表去容器中获取到所有的组件的名称，然后再进行遍历，这个过程是十分的消耗性能的，所以说Spring会在这里加入了保存切面信息的缓存。
					 * 但是事务功能不一样，事务模块的功能是直接去容器中获取Advisor类型的，选择范围小，基本不消耗性能，所以Spring在事务模块中没有加入缓存来保存我们的事务相关的advisor
					 */
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);

					// 遍历我们从IOC容器中获取处的所有Bean的名称，找出添加了@Aspect的类, 进行解析，注册到aspectNames列表
					for (String beanName : beanNames) {
						// 不合法的bean则略过，由子类定义规则，默认返回true
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
						// 通过beanName 获取对应的bean类型（class对象）
						// 获取类型，如果不存在，可能已经被代理过
						Class<?> beanType = this.beanFactory.getType(beanName, false);
						if (beanType == null) {
							continue;
						}
						// 根据class对象判断是否有切面 @Aspect
						if (this.advisorFactory.isAspect(beanType)) {
							// 证明是切面类

							//加入到缓存中
							aspectNames.add(beanName);
							// 创建切面元数据:保存bean类型以及@Aspect注解信息（把beanName和class对象构建成为一个AspectMetadata）
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							// 单例：检查@Aspect注解的value值，验证生成的增强是否是单例
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
								// 创建切面注解的实例工厂
								MetadataAwareAspectInstanceFactory factory = new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
								// classAdvisors集合存储的是该切面类中的所有的增强/通知{@link ReflectiveAspectJAdvisorFactory#getAdvisors(MetadataAwareAspectInstanceFactory)}
								// 真正的去获取我们的Advisor
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								// 如果bean是单例，则缓存bean的增强器
								if (this.beanFactory.isSingleton(beanName)) {
									this.advisorsCache.put(beanName, classAdvisors);
								}
								// bean非单例，只能缓存bean对应的增强器创建工厂
								else {
									this.aspectFactoryCache.put(beanName, factory);
								}
								advisors.addAll(classAdvisors);
							}
							// 切面创建模式非单例
							else {
								// Per target or per this.
								// 如果切面是非单例，但是bean是单例，抛出异常
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								this.aspectFactoryCache.put(beanName, factory);
								// 获取所有切面
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					this.aspectBeanNames = aspectNames;
					return advisors;
				}
			}
		}

		// 如果不是第一次解析切面，证明增强器已经被缓存过了，会执行下面的代码，查询缓存
		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		// 记录在缓存中
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			}
			else {
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
