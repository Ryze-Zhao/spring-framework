package org.springframework.zhao.initializing_bean2;


import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.zhao.initializing_bean.MyInitializingBean;


public class InitializingBeanTest2 {
    public static void main(String[] args) {
	    // 容器初始化阶段,保存 BeanDefinition
	    ClassPathResource resource = new ClassPathResource("spring/initializing_bean2/bean/SpringConfig.xml");
	    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
	    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
	    reader.loadBeanDefinitions(resource);

	    // 加载 Bean 阶段，如果第一次获取懒加载的Bean，现在加载。
	    MyInitializingBean2 myInitializingBean2 = factory.getBean(MyInitializingBean2.class);
	    System.out.println("name ：" + myInitializingBean2.getName());

    }
}
