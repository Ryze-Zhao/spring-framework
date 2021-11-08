package org.springframework.zhao.aware;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class AwareTest2 {
    public static void main(String[] args) {
	    ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring/aware/bean/SpringConfig.xml");
	    MyApplicationAware applicationAware = applicationContext.getBean(MyApplicationAware.class);
	    applicationAware.display();
    }
}
