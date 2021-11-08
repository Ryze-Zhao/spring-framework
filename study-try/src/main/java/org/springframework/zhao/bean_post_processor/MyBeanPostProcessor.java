package org.springframework.zhao.bean_post_processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("Bean [" + beanName + "] 开始初始化");
	    // 接口中的两个方法都要将传入的bean返回、而不能返回Null。否则getBean将无法取得目标。
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("Bean [" + beanName + "] 完成初始化");
	    // 接口中的两个方法都要将传入的bean返回、而不能返回Null。否则getBean将无法取得目标。
        return bean;
    }

    public void display(){
        System.out.println("hello BeanPostProcessor!!!");
    }
}