package org.springframework.zhao.life;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BeanLifeApplication {
	public static void main(String[] args) throws InterruptedException {
		// 为面试而准备的Bean生命周期加载过程
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring/life/bean/SpringConfig.xml");
		RyzeService ryzeService = context.getBean(RyzeService.class);
		System.out.println("RyzeService初始化完毕 属性RyzeName为：： " + ryzeService.getRyzeName());
		context.registerShutdownHook();
	}
}