package org.springframework.zhao.start;


import org.springframework.context.support.GenericGroovyApplicationContext;
import org.springframework.zhao.start.service.CityService;


public class GenericGroovyApplicationContextStartTest {
    public static void main(String[] args) {
    	// SpringConfig.groovy放在Resources路径中的目录即可
	    GenericGroovyApplicationContext context = new GenericGroovyApplicationContext("classpath:spring/start/bean/SpringConfig.groovy");
	    CityService cityService = context.getBean(CityService.class);
	    cityService.query();
    }
}
