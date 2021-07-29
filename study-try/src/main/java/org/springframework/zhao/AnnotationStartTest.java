package org.springframework.zhao;


import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.zhao.service.CityService;
import org.springframework.zhao.service.CityServiceImpl;


public class AnnotationStartTest {
    public static void main(String[] args) {
	    AnnotationConfigApplicationContext ac= new AnnotationConfigApplicationContext(ZhaoConfig.class);
		CityService cityService = (CityService) ac.getBean(CityServiceImpl.class);
		cityService.query();
    }
}
