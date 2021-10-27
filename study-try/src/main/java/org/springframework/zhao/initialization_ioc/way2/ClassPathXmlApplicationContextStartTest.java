package org.springframework.zhao.initialization_ioc.way2;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.zhao.start.service.CityService;


public class ClassPathXmlApplicationContextStartTest {
    public static void main(String[] args) {
    	// SpringConfig.xml放在Resources路径中的目录即可
	    ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring/initialization_ioc/way2/SpringConfig.xml");
	    WayService wayService = context.getBean(WayService.class);
		//  WayService wayService = (WayService) context.getBean("wayService");
	    wayService.query();
    }
}
