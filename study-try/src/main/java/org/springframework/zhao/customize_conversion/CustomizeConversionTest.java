package org.springframework.zhao.customize_conversion;

import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * 功能描述:Spring使用的三种方式
 *
 * @author : HeHaoZhao
 */
public class CustomizeConversionTest {
    public static void main(String[] args) {
	    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring/customize_conversion/SpringConfig.xml");
	    CityService cityService = context.getBean(CityService.class);
	    City city = cityService.getCity();
	    System.out.println(city.getName()+"-----"+city.getCode());
    }
}