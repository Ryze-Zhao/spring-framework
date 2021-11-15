package org.springframework.zhao.aop.demo03.service;

import org.springframework.stereotype.Service;

/**
 * 如果没实现接口，使用Cglib代理
 * @author : HeHaoZhao
 */
@Service
public class CglibAccountService {
	public void setQuery() {
		System.out.println("setQuery()--------------------");
	}
}