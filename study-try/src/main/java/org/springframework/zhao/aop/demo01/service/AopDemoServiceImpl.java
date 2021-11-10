package org.springframework.zhao.aop.demo01.service;

import org.springframework.stereotype.Service;

@Service
public class AopDemoServiceImpl implements AopDemoService {
    @Override
    public String query(String message) {
        System.out.println("AopDemoServiceImpl#query--------------------");
		return message;
    }
}