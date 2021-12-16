package org.springframework.zhao.aop.demo05;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.zhao.aop.demo05.service.AdviceService;

/**
 * 源码分析：多 AspectJ 顺序
 *
 * @author : HeHaoZhao
 */
public class SpringAop05Demo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(Demo05Config.class);
		AdviceService adviceService = ac.getBean(AdviceService.class);
		adviceService.setQuery();
	}
}