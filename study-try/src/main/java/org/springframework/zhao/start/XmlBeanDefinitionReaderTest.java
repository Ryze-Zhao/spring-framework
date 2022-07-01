package org.springframework.zhao.start;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.zhao.start.service.CityService;

/**
 * 功能描述:Spring使用的三种方式
 *
 * @author : HeHaoZhao
 */
public class XmlBeanDefinitionReaderTest {
    public static void main(String[] args) {
    	// 容器初始化阶段，保存 BeanDefinition
		ClassPathResource resource = new ClassPathResource("spring/start/bean/SpringConfig.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);

	    // 加载 Bean 阶段，如果第一次获取懒加载的Bean，现在加载。
	    CityService cityService = factory.getBean(CityService.class);
	    cityService.query();
    }
}