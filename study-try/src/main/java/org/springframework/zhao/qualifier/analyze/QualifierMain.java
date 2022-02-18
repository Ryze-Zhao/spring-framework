package org.springframework.zhao.qualifier.analyze;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class QualifierMain {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext configApplicationContext=new AnnotationConfigApplicationContext();
        configApplicationContext.register(QualifierConfig.class);
        configApplicationContext.refresh();
        People people = configApplicationContext.getBean(People.class);
        System.out.println(people.getLanguageList());
    }
}