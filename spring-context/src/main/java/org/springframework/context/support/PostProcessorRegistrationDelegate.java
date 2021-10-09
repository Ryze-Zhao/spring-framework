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
		// 存放 BeanFactoryPostProcessor、BeanDefinitionRegistryPostProcessor（也是继承BeanFactoryPostProcessor） 名称的集合
		Set<String> processedBeans = new HashSet<>();

		// 如果传入的 beanFactory 属于 BeanDefinitionRegistry 类型，那么进入处理
		// 否则即当前不属于 BeanDefinitionRegistry 类型，因此就直接执行传入的 List<BeanFactoryPostProcessor> beanFactoryPostProcessors的postProcessBeanFactory方法
		if (beanFactory instanceof BeanDefinitionRegistry) {

			// 由于上面已经判断了传入的beanFactory属于 BeanDefinitionRegistry 类型，此处可以直接强转而不报错
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			// 创建List<BeanFactoryPostProcessor> regularPostProcessors 用于存储 普通的BeanFactoryPostProcessor
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 创建List<BeanDefinitionRegistryPostProcessor> registryProcessors 用于存储 BeanDefinitionRegistryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
			// BeanDefinitionRegistryPostProcessor其实是BeanFactoryPostProcessor子接口，详细内容请参考我的文章


			// 循环遍历传入的 List<BeanFactoryPostProcessor> beanFactoryPostProcessors，将BeanDefinitionRegistryPostProcessor和普通BeanFactoryPostProcessor区分开
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				// 如果当前BeanFactoryPostProcessor postProcessor 的类型为 BeanDefinitionRegistryPostProcessor
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					// 由于上面已经判断了 BeanFactoryPostProcessor postProcessor 属于 BeanDefinitionRegistryPostProcessor 类型，此处可以直接强转而不报错
					BeanDefinitionRegistryPostProcessor registryProcessor = (BeanDefinitionRegistryPostProcessor) postProcessor;
					// 直接执行 BeanDefinitionRegistryPostProcessor 接口的 postProcessBeanDefinitionRegistry 方法
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 将当前BeanFactoryPostProcessor postProcessor 添加到 存储 BeanDefinitionRegistryPostProcessor 的集合 List<BeanDefinitionRegistryPostProcessor> registryProcessors
					registryProcessors.add(registryProcessor);
				}
				// 如果当前 BeanFactoryPostProcessor postProcessor 的类型 不 为 BeanDefinitionRegistryPostProcessor（那么就是普通的BeanFactoryPostProcessor）
				else {
					// 将当前 BeanFactoryPostProcessor postProcessor 添加到 存储 普通的BeanFactoryPostProcessor 的集合 List<BeanFactoryPostProcessor> regularPostProcessors
					regularPostProcessors.add(postProcessor);
				}
			}


			// Do not initialize FactoryBeans here: We need to leave all regular beans uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement PriorityOrdered, Ordered, and the rest.
			// 创建List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors 用于存储正在处理的 BeanDefinitionRegistryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();


			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			// 第一步：把类型为 BeanDefinitionRegistryPostProcessor 转为类型名并创建名称数组postProcessorNames（这是第一次处理）
			String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 遍历并找出所有实现 PriorityOrdered接口 的 BeanDefinitionRegistryPostProcessor 实现类
			for (String ppName : postProcessorNames) {
				// 如果该 BeanDefinitionRegistryPostProcessor 实现了排序优先级为最高级的PriorityOrdered接口 (PriorityOrdered为最高优先级,关于 BeanFactoryPostProcessor 优先级在后面会说)
				// 注：BeanDefinitionRegistryPostProcessor 是BeanFactoryPostProcessor子接口
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					// 根据类型名转为对应的 BeanDefinitionRegistryPostProcessor 存入List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 将已被处理的 BeanDefinitionRegistryPostProcessor 的类型名 存入 Set<String> processedBeans，避免后续重复执行
					processedBeans.add(ppName);
				}
			}
			// 排序currentRegistryProcessors（根据是否实现PriorityOrdered、Ordered接口和order值来排序）
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 将currentRegistryProcessors（当前所有正在处理的 BeanDefinitionRegistryPostProcessor） 添加到 存储 BeanDefinitionRegistryPostProcessor 的集合 List<BeanDefinitionRegistryPostProcessor> registryProcessors
			// 用于最后执行postProcessBeanFactory方法
			registryProcessors.addAll(currentRegistryProcessors);
			// 遍历 currentRegistryProcessors, 执行 postProcessBeanDefinitionRegistry 方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			// 清空 currentRegistryProcessors 所有元素(只删除元素不删除对象本身,集合对象长度为0且非null)
			currentRegistryProcessors.clear();


			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// 第二步：把类型为 BeanDefinitionRegistryPostProcessor 转为类型名并创建名称数组postProcessorNames（这是第二次处理）
			// 这里重复查找是因为执行完上面的 BeanDefinitionRegistryPostProcessor，能会新增了其他的BeanDefinitionRegistryPostProcessor, 因此需要重新查找
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 遍历并找出所有 不在processedBeans变量中且它的排序优先级为Ordered 的 BeanDefinitionRegistryPostProcessor 实现类
			for (String ppName : postProcessorNames) {
				// 如果该 BeanDefinitionRegistryPostProcessor 不在processedBeans变量中且它的排序优先级为Ordered (Ordered为第二优先级,关于 BeanFactoryPostProcessor 优先级在后面会说)
				// 注：BeanDefinitionRegistryPostProcessor 是BeanFactoryPostProcessor子接口
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					// 根据类型名转为对应的 BeanDefinitionRegistryPostProcessor 存入List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 将已被处理的 BeanDefinitionRegistryPostProcessor 的类型名 存入 Set<String> processedBeans，避免后续重复执行
					processedBeans.add(ppName);
				}
			}
			// 排序currentRegistryProcessors（根据是否实现PriorityOrdered、Ordered接口和order值来排序）
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 将currentRegistryProcessors（当前所有正在处理的 BeanDefinitionRegistryPostProcessor） 添加到 存储 BeanDefinitionRegistryPostProcessor 的集合 List<BeanDefinitionRegistryPostProcessor> registryProcessors
			// 用于最后执行postProcessBeanFactory方法
			registryProcessors.addAll(currentRegistryProcessors);
			// 遍历 currentRegistryProcessors, 执行 postProcessBeanDefinitionRegistry 方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			// 清空 currentRegistryProcessors 所有元素(只删除元素不删除对象本身,集合对象长度为0且非null)
			currentRegistryProcessors.clear();


			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 第三步：接下来就处理优先级最低的BeanDefinitionRegistryPostProcessor
			// 创建一个循环状态控制死循环
			boolean reiterate = true;
			// 创建一个死循环
			while (reiterate) {
				// 先把循环状态 定义为false
				reiterate = false;
				// 把类型为 BeanDefinitionRegistryPostProcessor 转为类型名并创建名称数组postProcessorNames（这是第三次处理）
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				// 遍历并找出所有 未被处理 的 BeanDefinitionRegistryPostProcessor 实现类
				for (String ppName : postProcessorNames) {
					// 如果此类型名没出现在processedBeans中（即：该BeanDefinitionRegistryPostProcessor未被处理过）
					if (!processedBeans.contains(ppName)) {
						// 根据类型名转为对应的 BeanDefinitionRegistryPostProcessor 存入List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						// processedBeans添加此后置处理器的名称
						processedBeans.add(ppName);
						// 循环状态重设reiterate=true,继续循环下去
						reiterate = true;
					}
				}
				// 排序currentRegistryProcessors（根据是否实现PriorityOrdered、Ordered接口和order值来排序）
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				// 将currentRegistryProcessors（当前所有正在处理的 BeanDefinitionRegistryPostProcessor） 添加到 存储 BeanDefinitionRegistryPostProcessor 的集合 List<BeanDefinitionRegistryPostProcessor> registryProcessors
				// 用于最后执行postProcessBeanFactory方法
				registryProcessors.addAll(currentRegistryProcessors);
				// 遍历 currentRegistryProcessors, 执行 postProcessBeanDefinitionRegistry 方法
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				// 清空 currentRegistryProcessors 所有元素(只删除元素不删除对象本身,集合对象长度为0且非null)
				currentRegistryProcessors.clear();
			}


			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 执行 List<BeanDefinitionRegistryPostProcessor> registryProcessors内所有元素的 postProcessBeanFactory 方法
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			// 执行	List<BeanFactoryPostProcessor> regularPostProcessors 内所有元素的 postProcessBeanFactory 方法
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}
		else {
			// Invoke factory processors registered with the context instance.
			// 如果不是 BeanDefinitionRegistryPostProcessor，那么就是 BeanFactoryPostProcessor 了，因此只需直接执行BeanFactoryPostProcessor实现类#postProcessBeanFactory 方法
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}


		// 到这里,入参 beanFactoryPostProcessors 和 容器中的所有 BeanDefinitionRegistryPostProcessor 已经全部处理完毕，下面开始处理容器中的所有BeanFactoryPostProcessor


		// Do not initialize FactoryBeans here: We need to leave all regular beans uninitialized to let the bean factory post-processors apply to them!
		// 把类型为 BeanFactoryPostProcessor 转为类型名并创建名称数组postProcessorNames（这是第一次处理）
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// 排序前，先需要分类。

		// 存储实现 PriorityOrdered 接口的 BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 存储实现 Ordered 接口的 BeanFactoryPostProcessor 的 BeanName
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 存储普通的 BeanFactoryPostProcessor 的 BeanName(就是没排序的咯)
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		// 遍历postProcessorNames
		for (String ppName : postProcessorNames) {
			// 如果该BeanFactoryPostProcessor 名已包含在processedBeans里，则什么都不处理（因为已经被处理过了）
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			// 如果 实现 PriorityOrdered 接口的 BeanFactoryPostProcessor
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 根据类型名转为对应的 BeanFactoryPostProcessor 存入 List<BeanFactoryPostProcessor> priorityOrderedPostProcessors
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			// 如果 实现 Ordered 接口的 BeanFactoryPostProcessor
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 将 BeanFactoryPostProcessor 对应的类型名 存入 List<String> orderedPostProcessorNames
				orderedPostProcessorNames.add(ppName);
			}
			// 其他 普通的 BeanFactoryPostProcessor
			else {
				// 将 BeanFactoryPostProcessor 对应的类型名 存入 List<String> nonOrderedPostProcessorNames
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
		// 定义List<BeanFactoryPostProcessor> orderedPostProcessors,长度为 orderedPostProcessorNames 的长度用于 存储 实现Ordered 接口的 BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		// 循环遍历orderedPostProcessorNames
		for (String postProcessorName : orderedPostProcessorNames) {
			// 根据类型名转为对应的 BeanFactoryPostProcessor 存入List<BeanFactoryPostProcessor> orderedPostProcessors
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 先排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 后注册（实现 Ordered 接口的 BeanFactoryPostProcessor）
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);


		// Finally, invoke all other BeanFactoryPostProcessors.
		// 第三步：注册 没有排序的 BeanPostProcessor
		// 定义List<BeanFactoryPostProcessor> nonOrderedPostProcessors,长度为 nonOrderedPostProcessorNames 的长度用于 存储普通的 BeanFactoryPostProcessor （未实现排序接口的）
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		//遍历 nonOrderedPostProcessorNames
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			// 根据类型名转为对应的 BeanFactoryPostProcessor 存入List<BeanFactoryPostProcessor> nonOrderedPostProcessors
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
