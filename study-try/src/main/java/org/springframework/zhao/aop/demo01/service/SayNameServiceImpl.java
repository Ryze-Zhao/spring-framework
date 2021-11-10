package org.springframework.zhao.aop.demo01.service;


public class SayNameServiceImpl implements SayNameService {

    @Override
    public String getName() {
        return "I am service";
    }
}