package org.springframework.zhao.bean_factory_post_processor;


import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;


public class BeanFactoryPostProcessorTest2 {
    public static void main(String[] args) {

	    ClassPathResource resource = new ClassPathResource("spring/bean_factory_post_processor/bean/SpringConfig.xml");
	    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
	    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
	    reader.loadBeanDefinitions(resource);
//		BeanFactoryPostProcessor_1 beanFactoryPostProcessor1 = new BeanFactoryPostProcessor_1();
//		beanFactoryPostProcessor1.postProcessBeanFactory(factory);
//		BeanFactoryPostProcessor_2 beanFactoryPostProcessor2 = new BeanFactoryPostProcessor_2();
//		beanFactoryPostProcessor2.postProcessBeanFactory(factory);


	    Student student = factory.getBean(Student.class);
		System.out.println("student name:" + student.getName() + "-- age:" + student.getAge());
    }
}
