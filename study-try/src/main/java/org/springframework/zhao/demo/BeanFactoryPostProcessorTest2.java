package org.springframework.zhao.demo;


import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

public class BeanFactoryPostProcessorTest2 {
    public static void main(String[] args) {
	    // 用于测试 PlaceholderConfigurerSupport ，通过 beanName.property=value 注入值
	    ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring/demo/test2/SpringConfig.xml");
	    Student student = applicationContext.getBean(Student.class);
	    System.out.println("student name:" + student.getName() + "-- age:" + student.getAge() + "-- properties:" + student.getProperties());
    }
}
