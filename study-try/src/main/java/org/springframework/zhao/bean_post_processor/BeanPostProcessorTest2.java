package org.springframework.zhao.bean_post_processor;


import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;


public class BeanPostProcessorTest2 {
	public static void main(String[] args) {
		// 容器初始化阶段，保存 BeanDefinition
		MyBeanPostProcessor myBeanPostProcessor = new MyBeanPostProcessor();
		ClassPathResource resource = new ClassPathResource("spring/bean_post_processor/bean/SpringConfig.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);

		factory.addBeanPostProcessor(myBeanPostProcessor);
		myBeanPostProcessor = factory.getBean(MyBeanPostProcessor.class);
		myBeanPostProcessor.display();
	}
}
