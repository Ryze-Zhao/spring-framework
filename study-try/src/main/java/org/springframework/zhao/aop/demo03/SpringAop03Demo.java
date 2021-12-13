package org.springframework.zhao.aop.demo03;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.zhao.aop.demo03.service.CglibAccountService;
import org.springframework.zhao.aop.demo03.service.JdkAccountService;

/**
 * 源码分析：JDK代理和CgLib代理
 *
 * @author : HeHaoZhao
 */
public class SpringAop03Demo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(Demo03Config.class);
		JdkAccountService jdkAccountService = ac.getBean(JdkAccountService.class);
//		CglibAccountService cglibAccountService = ac.getBean(CglibAccountService.class);
		jdkAccountService.setQuery();
//		cglibAccountService.setQuery();
	}
}