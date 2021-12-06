package org.springframework.zhao.circular_dependency;


import org.springframework.context.annotation.AnnotationConfigApplicationContext;



public class SpringDependDemo {
    public static void main(String[] args) {
	    AnnotationConfigApplicationContext context= new AnnotationConfigApplicationContext(ZhaoConfig.class);
    }
}
