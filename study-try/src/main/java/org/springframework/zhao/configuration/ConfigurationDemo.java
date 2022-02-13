package org.springframework.zhao.configuration;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 源码分析：多 AspectJ 顺序
 *
 * @author : HeHaoZhao
 */
public class ConfigurationDemo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(MyConfig.class);
	}
}