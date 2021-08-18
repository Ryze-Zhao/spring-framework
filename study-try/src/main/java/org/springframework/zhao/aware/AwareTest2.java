package org.springframework.zhao.aware;


import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;


public class AwareTest2 {
    public static void main(String[] args) {
	    ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring/aware/bean/SpringConfig.xml");
	    MyApplicationAware applicationAware = applicationContext.getBean(MyApplicationAware.class);
	    applicationAware.display();
    }
}
