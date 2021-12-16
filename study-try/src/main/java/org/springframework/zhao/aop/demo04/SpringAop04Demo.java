package org.springframework.zhao.aop.demo04;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.zhao.aop.demo04.service.AdviceService;

/**
 * 源码分析：Advice顺序
 *
 * @author : HeHaoZhao
 */
public class SpringAop04Demo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(Demo04Config.class);
		AdviceService adviceService = ac.getBean(AdviceService.class);
		adviceService.setQuery();
	}
}