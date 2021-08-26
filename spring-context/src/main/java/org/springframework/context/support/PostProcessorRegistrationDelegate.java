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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		// 存放bean后置处理器名称的集合
		Set<String> processedBeans = new HashSet<>();

		//判断传入的beanFactory是否属于BeanDefinitionRegistry类型,如果属于则进入处理，不属于直接执行方法
		if (beanFactory instanceof BeanDefinitionRegistry) {
			//由于上面已经判断了传入的beanFactory属于BeanDefinitionRegistry类型，此处可以直接强转而不报错
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			//创建bean后置处理器的集合<BeanFactoryPostProcessor>变量
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			//创建bean后置处理器的集合<BeanDefinitionRegistryPostProcessor>变量
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
			//BeanFactoryPostProcessor与BeanDefinitionRegistryPostProcessor的区别，可以参考我的问题

			//循环遍历传入的bean后置处理器
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				//如果该bean后置处理器的类型为BeanDefinitionRegistryPostProcessor
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					//该bean后置处理器强转类型BeanDefinitionRegistryPostProcessor
					BeanDefinitionRegistryPostProcessor registryProcessor =(BeanDefinitionRegistryPostProcessor) postProcessor;
					//集合<BeanDefinitionRegistryPostProcessor>变量添加此bean后置处理器
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					//集合<BeanFactoryPostProcessor>变量添加此工厂后置处理器
					registryProcessors.add(registryProcessor);
				}
				//如果该bean后置处理器的类型 不 为BeanDefinitionRegistryPostProcessor
				else {
					//只在集合<BeanFactoryPostProcessor>变量添加此bean后置处理器
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement PriorityOrdered, Ordered, and the rest.
			// 创建集合<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors变量(着重创建),后续用户执行后置处理器的 方法
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			// 把类型为BeanDefinitionRegistryPostProcessor的bean后置处理器的类型转为类型名并创建后置装载处理器名称的数组postProcessorNames
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 遍历每一个bean后置处理器
			for (String ppName : postProcessorNames) {
				//如果该bean后置处理器排序优先级为最高级(PriorityOrdered为最高优先级,关于bean后置处理器优先级在后面会说)
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//根据类型名转为bean后置处理器BeanDefinitionRegistryPostProcessor存入List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//上面的装载bean后置处理器的集合添加此bean后置处理器名字
					processedBeans.add(ppName);
				}
			}
			//排序currentRegistryProcessors
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//<BeanDefinitionRegistryPostProcessor> registryProcessors变量添加currentRegistryProcessors里面所有的元素
			registryProcessors.addAll(currentRegistryProcessors);
			//执行registryProcessors里面所有后续处理器的方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			//清除currentRegistryProcessors所有元素(只删除元素不删除对象本身,集合对象长度为0且非null)
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			//把所有BeanDefinitionRegistryPostProcessor的bean后置处理器的类型名提取出来存入postProcessorNames
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			//遍历所有的postProcessorNames数组元素
			for (String ppName : postProcessorNames) {
				//如果该bean后置处理器不在processedBeans变量中且该后置处理器的排序优先级为Ordered(第二优先级),则
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					//根据类型名转为bean后置处理器BeanDefinitionRegistryPostProcessor存入List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//processedBeans添加此后置处理器的名称
					processedBeans.add(ppName);
				}
			}
			// currentRegistryProcessors排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// registryProcessors添加currentRegistryProcessors排序所有的元素
			registryProcessors.addAll(currentRegistryProcessors);
			// 执行currentRegistryProcessors后置处理器集合变量排序里面所有的对应的方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			// 清除currentRegistryProcessors所有元素(只删除元素不删除对象本身,集合对象长度为0且非null)
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 接下来就处理优先级最低的

			// 创建一个循环状态控制死循环
			boolean reiterate = true;

			// 创建一个死循环
			while (reiterate) {
				// 先把循环状态 定义为false
				reiterate = false;
				//把所有BeanDefinitionRegistryPostProcessor的bean后置处理器的类型名提取出来存入postProcessorNames
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				//循环postProcessorNames数组
				for (String ppName : postProcessorNames) {
					//如果此类型名没出现载processedBeans中,则
					if (!processedBeans.contains(ppName)) {
						//根据类型名转为bean后置处理器BeanDefinitionRegistryPostProcessor存入<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors	currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						//processedBeans添加此后置处理器的名称
						processedBeans.add(ppName);
						//循环状态重设reiterate=true,继续循环下去
						reiterate = true;
					}
				}
				//排序sortPostProcessors
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				//<BeanDefinitionRegistryPostProcessor> registryProcessors(bean后置处理器集合)放入currentRegistryProcessors的所有元素
				registryProcessors.addAll(currentRegistryProcessors);
				//调用currentRegistryProcessors中所有后置处理器的方法
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				//清除currentRegistryProcessors所有元素(只删除元素不删除对象本身,集合对象长度为0且非null)
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			//执行registryProcessors内的所有元素(后置处理器)方法
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			//执行regularPostProcessors内的所有元素(后置处理器)的方法
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			//直接执行bean后置处理器方法
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans uninitialized to let the bean factory post-processors apply to them!
		// 把bean后置处理器的类型为BeanFactoryPostProcessor的所有类型名转为字符串并创建postProcessorNames数组装载
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.

		//剩下的代码执行上面那一堆都没能执行到的bean后置处理器
		// 排序前，先需要分类。

		// 实现 PriorityOrdered 接口的 BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 实现 Ordered 接口的 BeanFactoryPostProcessor 的 BeanName
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 普通的 BeanFactoryPostProcessor 的 BeanName(就是没排序的咯)
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		// 遍历postProcessorNames
		for (String ppName : postProcessorNames) {
			// 如果该bean后置处理器名已包含在processedBeans里，则什么都不处理
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			// 如果 实现 PriorityOrdered 接口的 BeanFactoryPostProcessor
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			// 如果 实现 Ordered 接口的 BeanFactoryPostProcessor
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			// 其他 普通的 BeanFactoryPostProcessor
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		// 第一步：注册所有实现了 PriorityOrdered 的 BeanFactoryPostProcessor
		// 先排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 后注册（注册实现 PriorityOrdered 接口的 BeanFactoryPostProcessor），底层循环集合
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		// 第二步：注册实现 Ordered 接口的 BeanFactoryPostProcessor
		// 先定义一个集合变量装载bean后置处理器<BeanFactoryPostProcessor> orderedPostProcessors,长度为orderedPostProcessorNames的长度
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		// 循环遍历orderedPostProcessorNames
		for (String postProcessorName : orderedPostProcessorNames) {
			//根据处理器名字反射出该处理器的对象并放入orderedPostProcessors
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}

		// 先排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 后注册（实现 Ordered 接口的 BeanFactoryPostProcessor）
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		// 第三步：注册 没有排序的 BeanPostProcessor
		//定义一个集合变量装载bean后置处理器<BeanFactoryPostProcessor> nonOrderedPostProcessors,其长度=nonOrderedPostProcessorNames(最次优先级的bean后置处理器名的数组)长度
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		//遍历nonOrderedPostProcessorNames
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			//根据bean后置处理器名称反射出后置处理器对象并放入nonOrderedPostProcessors
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}

		// 注册，无需排序
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have modified the original metadata, e.g. replacing placeholders in values...
		// 清除缓存的合并bean定义，因为后处理器可能修改了原始元数据，例如替换值中的占位符。。。
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// 从 beanDefinitionNames 中获取类型所有的 BeanPostProcessor 的 beanName
		// 虽然这些 beanName 都已经全部加载到容器中去，但是没有实例化 bean
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		// 记录所有的beanProcessor数量
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		// 注册 BeanPostProcessorChecker，它主要是用于在 BeanPostProcessor 实例化期间记录日志，并且 BeanPostProcessorChecker 实现了 BeanPostProcessor接口
		// 可用来判断当前 bean 是否已经执行了所有的 BeanPostProcessor
		// 当 Spring 中高配置的后置处理器还没有注册就已经开始了 bean 的实例化过程，这个时候便会打印 BeanPostProcessorChecker 中的内容
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// 对 BeanPostProcessor 进行分类排序
		// Separate between BeanPostProcessors that implement PriorityOrdered,Ordered, and the rest.
		// 实现 PriorityOrdered 接口的 BeanPostProcessor
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 实现 MergedBeanDefinitionPostProcessor 接口的 BeanPostProcessor
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		// 实现 Ordered 接口的 BeanPostProcessor 的 BeanName
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 普通的 BeanPostProcessor 的 BeanName(就是没排序的咯)
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();


		// 根据postProcessorNames开始处理，加入到各个分类中
		for (String ppName : postProcessorNames) {
			// PriorityOrdered
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 调用 getBean 获取 bean 实例对象
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 有序 Ordered
				orderedPostProcessorNames.add(ppName);
			}
			else {
				// 无序
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		// 第一步：注册所有实现了 PriorityOrdered 的 BeanPostProcessor
		// 先排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 后注册（注册实现 PriorityOrdered 接口的 BeanPostProcessor），底层循环List调用 beanFactory.addBeanPostProcessor(postProcessor); 方法
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);



		// Next, register the BeanPostProcessors that implement Ordered.
		// 第二步：注册实现 Ordered 接口的 BeanPostProcessor
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		// 先排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 后注册（实现 Ordered 接口的 BeanPostProcessor）
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);



		// Now, register all regular BeanPostProcessors.
		// 第三步：注册 没有排序的 BeanPostProcessor
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		// 注册，无需排序
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);



		// Finally, re-register all internal BeanPostProcessors.
		// 最后，注册实现所有 MergedBeanDefinitionPostProcessor 接口的 BeanPostProcessor
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		// 加入ApplicationListenerDetector（探测器）
		// 重新注册 BeanPostProcessor 以检测内部 bean，因为 ApplicationListeners 将其移动到处理器链的末尾
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		// 无需排序?
		if (postProcessors.size() <= 1) {
			return;
		}
		// 获取比较器
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		// 排序
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanDefinitionRegistry(registry);
			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanFactory(beanFactory);
			postProcessBeanFactory.end();
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		}
		else {
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
