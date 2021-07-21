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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * <p>This is just a minimal interface: The main intention is to allow a
 * {@link BeanFactoryPostProcessor} to introspect and modify property values
 * and other bean metadata.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 19.03.2004
 * @see ConfigurableListableBeanFactory#getBeanDefinition
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

	/**
	 * Scope identifier for the standard singleton scope: {@value}.<p>Note that extended bean factories might support further scopes.
	 * 标准单例作用域的作用域标识符：{@value}。<p>请注意，扩展bean工厂可能支持更多的作用域。
	 *
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
	 */
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * Scope identifier for the standard prototype scope: {@value}.<p>Note that extended bean factories might support further scopes.
	 * 标准原型作用域的作用域标识符：{@value}。<p>请注意，扩展bean工厂可能支持更多的作用域。
	 *
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 */
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


	/**
	 * Role hint indicating that a {@code BeanDefinition} is a major part of the application. Typically corresponds to a user-defined bean.
	 * 角色提示，指示{@codebeandefinition}是应用程序的主要部分。通常对应于用户定义的bean。
	 */
	int ROLE_APPLICATION = 0;

	/**
	 * Role hint indicating that a {@code BeanDefinition} is a supporting
	 * part of some larger configuration, typically an outer
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}.
	 * {@code SUPPORT} beans are considered important enough to be aware
	 * of when looking more closely at a particular
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition},
	 * but not when looking at the overall configuration of an application.
	 * 角色提示，指示{@code BeanDefinition}是支持某些较大配置的一部分，通常是外部配置
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}。
	 * {@code SUPPORT}bean被认为是非常重要的更仔细地观察某个特定的
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}，但在查看应用程序的总体配置时就不是了。
	 */
	int ROLE_SUPPORT = 1;

	/**
	 * Role hint indicating that a {@code BeanDefinition} is providing an
	 * entirely background role and has no relevance to the end-user. This hint is
	 * used when registering beans that are completely part of the internal workings
	 * of a {@link org.springframework.beans.factory.parsing.ComponentDefinition}.
	 * 角色提示，指示{@code BeanDefinition}正在提供
	 * 完全是后台角色，与最终用户无关。这个暗示是
	 * 在注册完全属于内部工作的bean时使用
	 * 一个{@link org.springframework.beans.factory.parsing.ComponentDefinition}。
	 */
	int ROLE_INFRASTRUCTURE = 2;


	// Modifiable attributes

	/**
	 * Set the name of the parent definition of this bean definition, if any.
	 * 设置此bean定义的父定义的名称（如果有）。
	 */
	void setParentName(@Nullable String parentName);

	/**
	 * Return the name of the parent definition of this bean definition, if any.
	 * 返回此bean定义的父定义的名称（如果有）。
	 */
	@Nullable
	String getParentName();

	/**
	 * Specify the bean class name of this bean definition.<p>The class name can be modified during bean factory post-processing,typically replacing the original class name with a parsed variant of it.
	 * 指定此bean定义的bean类名。<p>可以在bean工厂后处理过程中修改类名，通常用解析后的变体替换原始类名。
	 *
	 * @see #setParentName
	 * @see #setFactoryBeanName
	 * @see #setFactoryMethodName
	 */
	void setBeanClassName(@Nullable String beanClassName);

	/**
	 * Return the current bean class name of this bean definition.<p>Note that this does not have to be the actual class name used at runtime, in
	 * case of a child definition overriding/inheriting the class name from its parent.Also, this may just be the class that a factory method is called on, or it may
	 * even be empty in case of a factory bean reference that a method is called on.Hence, do <i>not</i> consider this to be the definitive bean type at runtime but
	 * rather only use it for parsing purposes at the individual bean definition level.
	 * 返回此bean定义的当前bean类名。<p>请注意，它不必是运行时使用的实际类名子定义重写从其父级继承类名的情况。而且，这可能只是调用工厂方法的类，也可能是
	 * 在工厂bean引用的情况下，即使是调用方法，也可以是空的。因此，【不要】认为这是运行时的最终bean类型，而是仅在单个bean定义级别将其用于解析目的。
	 *
	 *
	 * @see #getParentName()
	 * @see #getFactoryBeanName()
	 * @see #getFactoryMethodName()
	 */
	@Nullable
	String getBeanClassName();

	/**
	 * Override the target scope of this bean, specifying a new scope name.
	 * 重写这个bean的目标作用域，指定一个新的作用域名称。
	 *
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	void setScope(@Nullable String scope);

	/**
	 * Return the name of the current target scope for this bean,or {@code null} if not known yet.
	 * 返回这个bean的当前目标作用域的名称，或者{@code null}（如果不知道的话）
	 */
	@Nullable
	String getScope();

	/**
	 * Set whether this bean should be lazily initialized.<p>If {@code false}, the bean will get instantiated on startup by bean factories that perform eager initialization of singletons.
	 * 设置此bean是否应该延迟初始化。<p>如果{@code false}，则bean将在启动时由执行单例急切初始化的bean工厂实例化。
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * Return whether this bean should be lazily initialized, i.e. not eagerly instantiated on startup. Only applicable to a singleton bean.
	 * 返回是否应该延迟初始化这个bean，即在启动时不需要立即实例化。只适用于单例bean。
	 */
	boolean isLazyInit();

	/**
	 * Set the names of the beans that this bean depends on being initialized.The bean factory will guarantee that these beans get initialized first.
	 * 设置此bean依赖于初始化的bean的名称。bean工厂将保证这些bean首先得到初始化。
	 */
	void setDependsOn(@Nullable String... dependsOn);

	/**
	 * Return the bean names that this bean depends on.
	 * 返回此bean所依赖的bean名称。
	 */
	@Nullable
	String[] getDependsOn();

	/**
	 * Set whether this bean is a candidate for getting autowired into some other bean.
	 * <p>Note that this flag is designed to only affect type-based autowiring.
	 * It does not affect explicit references by name, which will get resolved even
	 * if the specified bean is not marked as an autowire candidate. As a consequence,
	 * autowiring by name will nevertheless inject a bean if the name matches.
	 * 设置这个bean是否可以autowired到其他bean中。 <p>请注意，此标志设计为仅影响基于类型的autowiring。
	 * 它不影响显式引用的名称，这将得到解决，甚至如果指定的bean没有标记为autowire候选。因此，如果名称匹配，按名称自动连线将注入bean。
	 */
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * Return whether this bean is a candidate for getting autowired into some other bean.
	 * 返回这个bean是否是autowired到其他bean的候选者。
	 */
	boolean isAutowireCandidate();

	/**
	 * Set whether this bean is a primary autowire candidate.<p>If this value is {@code true} for exactly one bean among multiple matching candidates, it will serve as a tie-breaker.
	 * 设置这个bean是否是主要的autowire候选者。<p>如果这个值对于多个匹配候选者中的一个bean来说是{@code true}，它将充当一个断开连接的工具。
	 */
	void setPrimary(boolean primary);

	/**
	 * Return whether this bean is a primary autowire candidate.
	 * 返回此bean是否是主要autowire候选。
	 */
	boolean isPrimary();

	/**
	 * Specify the factory bean to use, if any.This the name of the bean to call the specified factory method on.
	 * 指定要使用的工厂bean（如果有）。这是要调用指定工厂方法的bean的名称。
	 *
	 * @see #setFactoryMethodName
	 */
	void setFactoryBeanName(@Nullable String factoryBeanName);

	/**
	 * Return the factory bean name, if any.
	 * 返回工厂bean名称（如果有）。
	 */
	@Nullable
	String getFactoryBeanName();

	/**
	 * Specify a factory method, if any. This method will be invoked with constructor arguments, or with no arguments if none are specified.
	 * The method will be invoked on the specified factory bean, if any,or otherwise as a static method on the local bean class.
	 * 指定工厂方法（如果有）。此方法将通过调用构造函数参数，如果未指定参数，则不带参数。
	 * 方法将在指定的工厂bean上调用，如果有的话，或者作为本地bean类上的静态方法。
	 *
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	void setFactoryMethodName(@Nullable String factoryMethodName);

	/**
	 * Return a factory method, if any.
	 * 返回工厂方法（如果有）
	 */
	@Nullable
	String getFactoryMethodName();

	/**
	 * Return the constructor argument values for this bean.<p>The returned instance can be modified during bean factory post-processing.
	 * 返回此bean的构造函数参数值。<p>返回的实例可以在bean工厂后处理期间修改。
	 *
	 * @return the ConstructorArgumentValues object (never {@code null})
	 */
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * Return if there are constructor argument values defined for this bean.
	 * 如果有为此bean定义的构造函数参数值，则返回。
	 *
	 * @since 5.0.2
	 */
	default boolean hasConstructorArgumentValues() {
		return !getConstructorArgumentValues().isEmpty();
	}

	/**
	 * Return the property values to be applied to a new instance of the bean.<p>The returned instance can be modified during bean factory post-processing.
	 * 返回要应用于bean的新实例的属性值。<p>返回的实例可以在bean工厂后处理期间修改。
	 *
	 * @return the MutablePropertyValues object (never {@code null})
	 */
	MutablePropertyValues getPropertyValues();

	/**
	 * Return if there are property values defined for this bean.
	 * 如果有为此bean定义的属性值，则返回。
	 *
	 * @since 5.0.2
	 */
	default boolean hasPropertyValues() {
		return !getPropertyValues().isEmpty();
	}

	/**
	 * Set the name of the initializer method.
	 * 设置初始值设定项方法的名称。
	 * @since 5.1
	 */
	void setInitMethodName(@Nullable String initMethodName);

	/**
	 * Return the name of the initializer method.
	 * 返回初始值设定项方法的名称。
	 * @since 5.1
	 */
	@Nullable
	String getInitMethodName();

	/**
	 * Set the name of the destroy method.
	 * 设置销毁方法的名称。
	 * @since 5.1
	 */
	void setDestroyMethodName(@Nullable String destroyMethodName);

	/**
	 * Return the name of the destroy method.
	 * 返回销毁方法的名称。
	 * @since 5.1
	 */
	@Nullable
	String getDestroyMethodName();

	/**
	 * Set the role hint for this {@code BeanDefinition}. The role hint provides the frameworks as well as tools an indication of the role and importance of a particular {@code BeanDefinition}.
	 * 为此{@code BeanDefinition}设置角色提示。角色提示为框架和工具提供了特定{@codebeandefinition}的角色和重要性的指示。
	 * @since 5.1
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	void setRole(int role);

	/**
	 * Get the role hint for this {@code BeanDefinition}. The role hint provides the frameworks as well as tools an indication of the role and importance of a particular {@code BeanDefinition}.
	 * 获取此{@code BeanDefinition}的角色提示。角色提示为框架和工具提供了特定{@codebeandefinition}的角色和重要性的指示。
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	int getRole();

	/**
	 * Set a human-readable description of this bean definition.
	 * 设置此bean定义的可读描述。
	 * @since 5.1
	 */
	void setDescription(@Nullable String description);

	/**
	 * Return a human-readable description of this bean definition.
	 * 返回此bean定义的可读描述
	 */
	@Nullable
	String getDescription();


	// Read-only attributes

	/**
	 * Return a resolvable type for this bean definition,based on the bean class or other specific metadata.
	 * <p>This is typically fully resolved on a runtime-merged bean definition but not necessarily on a configuration-time definition instance.
	 * 基于bean类或其他特定元数据，返回此bean定义的可解析类型。
	 * <p>这通常在运行时合并的bean定义上完全解决，但不一定在配置时定义实例上解决。
	 * @return the resolvable type (potentially {@link ResolvableType#NONE})
	 * @since 5.2
	 * @see ConfigurableBeanFactory#getMergedBeanDefinition
	 */
	ResolvableType getResolvableType();

	/**
	 * Return whether this a <b>Singleton</b>, with a single, shared instance returned on all calls.
	 * 返回是否是一个<b>单例<b>，所有调用都返回一个共享实例。
	 * @see #SCOPE_SINGLETON
	 */
	boolean isSingleton();

	/**
	 * Return whether this a <b>Prototype</b>, with an independent instance returned for each call.
	 * 返回是否是一个<b>原型<b>，并为每个调用返回一个独立的实例。
	 * @since 3.0
	 * @see #SCOPE_PROTOTYPE
	 */
	boolean isPrototype();

	/**
	 * Return whether this bean is "abstract", that is, not meant to be instantiated.
	 * 返回这个bean是否是“抽象的”，也就是说，不是要实例化的。
	 */
	boolean isAbstract();

	/**
	 * Return a description of the resource that this bean definition came from (for the purpose of showing context in case of errors).
	 * 返回此bean定义所来自的资源的描述（用于在出现错误时显示上下文）。
	 */
	@Nullable
	String getResourceDescription();

	/**
	 * Return the originating BeanDefinition, or {@code null} if none.<p>Allows for retrieving the decorated bean definition, if any.
	 * <p>Note that this method returns the immediate originator. Iterate through the originator chain to find the original BeanDefinition as defined by the user.
	 * 返回原始bean定义，如果没有，{@code null}。<p>允许检索修饰bean定义（如果有的话）。
	 * <p>请注意，此方法返回直接发起人。遍历发起者链以查找用户定义的原始BeanDefinition。
	 */
	@Nullable
	BeanDefinition getOriginatingBeanDefinition();

}
