package org.springframework.zhao.initialization_ioc.way1;


public class WayServiceImpl implements WayService {
    @Override
    public void query() {
        System.out.println("spring-init--------------------");
    }
}