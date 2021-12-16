package org.springframework.zhao.aop.demo05.service;

import org.springframework.stereotype.Service;

@Service
public class AdviceServiceImpl implements AdviceService {
    @Override
    public void setQuery() {
//	    System.out.println(10/0);
	    System.out.println("setQuery()--------------------");
    }
}