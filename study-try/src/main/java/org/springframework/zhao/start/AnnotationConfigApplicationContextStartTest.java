package org.springframework.zhao.start;


import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.zhao.start.config.ZhaoConfig;
import org.springframework.zhao.start.service.CityService;
import org.springframework.zhao.start.service.CityServiceImpl;


public class AnnotationConfigApplicationContextStartTest {
    public static void main(String[] args) {
    	// ZhaoConfig需要添加 @Configuration 与 @ComponentScan("org.springframework.zhao") 注解

	    // 方式一
	    AnnotationConfigApplicationContext context= new AnnotationConfigApplicationContext(ZhaoConfig.class);
		CityService cityService = context.getBean(CityServiceImpl.class);
		cityService.query();


	    // 方式二
//	    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
//	    context.register(ZhaoConfig.class);
//	    context.refresh();
//	    CityService cityService =context.getBean(CityServiceImpl.class);
//	    cityService.query();
    }
}
