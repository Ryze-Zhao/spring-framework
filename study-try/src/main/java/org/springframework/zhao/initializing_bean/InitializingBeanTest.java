package org.springframework.zhao.initializing_bean;


import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;


public class InitializingBeanTest {
    public static void main(String[] args) {
	    // 容器初始化阶段,保存 BeanDefinition
	    ClassPathResource resource = new ClassPathResource("spring/initializing_bean/bean/SpringConfig.xml");
	    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
	    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
	    reader.loadBeanDefinitions(resource);

	    // 加载 Bean 阶段，如果第一次获取懒加载的Bean，现在加载。
	    MyInitializingBean myInitializingBean = factory.getBean(MyInitializingBean.class);
	    System.out.println("name ：" + myInitializingBean.getName());

    }
}
