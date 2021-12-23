package org.springframework.zhao.custom_servlet_3;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

public class MyCustomWebApplicationInitializer implements CustomWebApplicationInitializer {
    @Override
    public void onStartup(ServletContext servletContext) {

		System.out.println("web容器初始化回调，可在此添加spring容器启动代码");

	    // 实例化spring容器
	    AnnotationConfigWebApplicationContext ac = new AnnotationConfigWebApplicationContext();
	    ac.register(WebConfig.class);
//		ac.refresh();

	    // DispatcherServlet注册到web容器

	    // servlet
	    DispatcherServlet servlet = new DispatcherServlet(ac);
	    ServletRegistration.Dynamic registration = servletContext.addServlet(DispatcherServlet.class.getName(), servlet);
	    registration.setLoadOnStartup(1);
	    // servlet-mapping
	    registration.addMapping("/");
    }
}