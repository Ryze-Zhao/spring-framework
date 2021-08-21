package org.springframework.zhao.property_resource_configurer;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class PropertySourcesPlaceholderConfigurerTest {
	public static void main(String[] args) {
		// 用于测试 PropertyOverrideConfigurer ，通过${}注入值
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring/property_resource_configurer/test1/SpringConfig.xml");

		Student student = applicationContext.getBean(Student.class);
		System.out.println("student name:" + student.getName() + "-- age:" + student.getAge() + "-- properties:" + student.getProperties());
	}
}
