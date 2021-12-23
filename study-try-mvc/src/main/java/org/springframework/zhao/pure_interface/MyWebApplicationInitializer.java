package org.springframework.zhao.pure_interface;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * .
 * 从官方文档拷贝，主要目的初始化spring容器，注册servlet和注册servlet-mapping
 * @author : HeHaoZhao
 */
public class MyWebApplicationInitializer implements WebApplicationInitializer {
	/**
	 * .
	 * ServletContext代替web.xml
	 */
	@Override
	public void onStartup(ServletContext servletCxt) {
		// 实例化spring容器
		AnnotationConfigWebApplicationContext ac = new AnnotationConfigWebApplicationContext();
		ac.register(WebConfig.class);
//		ac.refresh();

		// DispatcherServlet注册到web容器

		// servlet
		DispatcherServlet servlet = new DispatcherServlet(ac);
		ServletRegistration.Dynamic registration = servletCxt.addServlet(DispatcherServlet.class.getName(), servlet);
		registration.setLoadOnStartup(1);
		// servlet-mapping
		registration.addMapping("/");
	}
}