package org.springframework.zhao.bean_factory_post_processor;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class BeanFactoryPostProcessorTest {
	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring/bean_factory_post_processor/bean/SpringConfig.xml");

		Student student = applicationContext.getBean(Student.class);
		System.out.println("student name:" + student.getName() + "-- age:" + student.getAge());
	}
}
