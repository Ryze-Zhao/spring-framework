package org.springframework.zhao.start;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.zhao.start.service.CityService;
import org.springframework.zhao.start.service.CityServiceImpl;

public class GenericApplicationContextStartTest {
	public static void main(String[] args) {
		// 方式一
//		GenericApplicationContext context = new GenericApplicationContext();
//		XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(context);
//		xmlReader.loadBeanDefinitions(new ClassPathResource("spring/start/bean/SpringConfig.xml"));
//		// refresh()只允许调用一次
//		context.refresh();
//		CityService cityService = (CityService) context.getBean(CityService.class);
//		cityService.query();

		// 方式二
		//	创建GenericApplicationContext对象
		GenericApplicationContext context = new GenericApplicationContext();
		//	调用context的方法对象注册
		context.refresh();//刷新
		//	将new出来的User对象在Spring容器中完成注册
		context.registerBean(CityService.class, CityServiceImpl::new);
		//	获取在Spring5中注册的对象
		CityService cityService = context.getBean(CityService.class);
		cityService.query();
	}
}