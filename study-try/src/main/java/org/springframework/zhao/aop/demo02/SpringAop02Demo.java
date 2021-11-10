package org.springframework.zhao.aop.demo02;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.zhao.aop.demo02.service.AccountService;

public class SpringAop02Demo {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ac= new AnnotationConfigApplicationContext(ZhaoConfig.class);
        AccountService bean = ac.getBean(AccountService.class);
        bean.query();
        bean.query1("aaaa",11);
        bean.setQuery();
    }
}