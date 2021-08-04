package org.springframework.zhao.start;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.zhao.start.config.ZhaoConfig;

/**
 * 功能描述:Spring使用的三种方式
 *
 * @author : HeHaoZhao
 */
public class SpringStartWay {
    public static void main(String[] args) {
	    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:application.xml");
	    FileSystemXmlApplicationContext ctx = new FileSystemXmlApplicationContext("spring-config.xml");
	    AnnotationConfigApplicationContext ac= new AnnotationConfigApplicationContext(ZhaoConfig.class);
    }
}