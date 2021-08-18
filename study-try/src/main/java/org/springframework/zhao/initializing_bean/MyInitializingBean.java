package org.springframework.zhao.initializing_bean;

import org.springframework.beans.factory.InitializingBean;

public class MyInitializingBean implements InitializingBean {

    private String name;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("MyInitializingBean initializing...");
        this.name = "MyInitializingBean$Name$Code";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}



