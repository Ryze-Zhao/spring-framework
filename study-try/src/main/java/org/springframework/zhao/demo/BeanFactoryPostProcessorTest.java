package org.springframework.zhao.demo;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class BeanFactoryPostProcessorTest {
	public static void main(String[] args) {
		// 用于测试 PropertyOverrideConfigurer ，通过${}注入值
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring/demo/test1/SpringConfig.xml");

		Student student = applicationContext.getBean(Student.class);
		System.out.println("student name:" + student.getName() + "-- age:" + student.getAge() + "-- properties:" + student.getProperties());
	}
}
