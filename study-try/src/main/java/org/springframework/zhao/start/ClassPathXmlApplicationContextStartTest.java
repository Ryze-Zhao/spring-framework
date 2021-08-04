package org.springframework.zhao.start;


import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.zhao.start.service.CityService;


public class ClassPathXmlApplicationContextStartTest {
    public static void main(String[] args) {
    	// SpringConfig.xml放在Resources路径中的目录即可
	    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring/start/bean/SpringConfig.xml");
	    CityService cityService = context.getBean(CityService.class);
		//  CityService cityService = (CityService) context.getBean("cityService");
	    cityService.query();
    }
}
