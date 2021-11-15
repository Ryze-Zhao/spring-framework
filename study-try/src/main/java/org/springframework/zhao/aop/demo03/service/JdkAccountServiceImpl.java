package org.springframework.zhao.aop.demo03.service;

import org.springframework.stereotype.Service;

@Service
public class JdkAccountServiceImpl implements JdkAccountService {
    @Override
    public void setQuery() {
	    System.out.println("setQuery()--------------------");
    }
}