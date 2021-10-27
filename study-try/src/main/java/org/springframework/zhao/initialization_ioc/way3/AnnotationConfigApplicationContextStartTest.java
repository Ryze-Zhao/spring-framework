package org.springframework.zhao.initialization_ioc.way3;


import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;



public class AnnotationConfigApplicationContextStartTest {
    public static void main(String[] args) {
    	// ZhaoConfig需要添加 @Configuration 与 @ComponentScan("org.springframework.zhao.initialization_ioc.way3") 注解

	    // 方式一
	    ApplicationContext context= new AnnotationConfigApplicationContext(ZhaoConfig.class);
		WayService wayService = context.getBean(WayServiceImpl.class);
		wayService.query();


	    // 方式二
//	    ApplicationContext context = new AnnotationConfigApplicationContext();
//	    context.register(ZhaoConfig.class);
//	    context.refresh();
//	    WayService wayService =context.getBean(WayServiceImpl.class);
//	    wayService.query();
    }
}
