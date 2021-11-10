package org.springframework.zhao.aop.demo02.service;

import org.springframework.stereotype.Service;
import org.springframework.zhao.aop.demo02.MyAnnotation;

@Service
@MyAnnotation
public class AccountServiceImpl implements AccountService {
    @Override
    public void query() {
        System.out.println("query()--------------------");
    }
    @Override
    public void query1(String aa,int bb) {
        System.out.println("query1()--------------------");
    }
    @Override
    @MyAnnotation
    public void setQuery() {
        System.out.println("setQuery()--------------------");
    }
}