package org.springframework.zhao.bean_post_processor;


import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.zhao.aware.MyApplicationAware;


public class BeanPostProcessorTest {
    public static void main(String[] args) {
	    // 容器初始化阶段,保存 BeanDefinition
	    ClassPathResource resource = new ClassPathResource("spring/bean_post_processor/bean/SpringConfig.xml");
	    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
	    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
	    reader.loadBeanDefinitions(resource);

	    // 加载 Bean 阶段，如果第一次获取懒加载的Bean，现在加载。
	    MyBeanPostProcessor myBeanPostProcessor = factory.getBean(MyBeanPostProcessor.class);
	    myBeanPostProcessor.display();
    }
}
