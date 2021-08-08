package org.springframework.zhao.start;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.zhao.start.config.ZhaoConfig;

/**
 * 功能描述:Spring使用的三种方式
 *
 * @author : HeHaoZhao
 */
public class XmlBeanDefinitionReaderTest {
    public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("spring/start/bean/SpringConfig.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);
    }
}