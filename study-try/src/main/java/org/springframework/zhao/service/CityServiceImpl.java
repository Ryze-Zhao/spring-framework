package org.springframework.zhao.service;

import org.springframework.stereotype.Service;

@Service
public class CityServiceImpl implements CityService {
    @Override
    public void query() {
        System.out.println("spring-init--------------------");
    }
}