package org.springframework.zhao.property_resource_configurer;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PropertyResourceConfigurerTest2 {
    public static void main(String[] args) {
	    // 用于测试 PlaceholderConfigurerSupport ，通过 beanName.property=value 注入值
	    ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring/property_resource_configurer/test2/SpringConfig.xml");
	    Student student = applicationContext.getBean(Student.class);
	    System.out.println("student name:" + student.getName() + "-- age:" + student.getAge() + "-- properties:" + student.getProperties());
    }
}
