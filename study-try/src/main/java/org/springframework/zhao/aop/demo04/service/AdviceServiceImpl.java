package org.springframework.zhao.aop.demo04.service;

import org.springframework.stereotype.Service;

@Service
public class AdviceServiceImpl implements AdviceService {
    @Override
    public void setQuery() {
	    System.out.println("setQuery()--------------------");
    }
}