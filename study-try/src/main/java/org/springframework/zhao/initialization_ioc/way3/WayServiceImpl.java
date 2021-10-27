package org.springframework.zhao.initialization_ioc.way3;


import org.springframework.stereotype.Service;

@Service
public class WayServiceImpl implements WayService {
    @Override
    public void query() {
        System.out.println("spring-init--------------------");
    }
}