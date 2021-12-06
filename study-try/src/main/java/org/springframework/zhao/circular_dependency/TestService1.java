package org.springframework.zhao.circular_dependency;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestService1 {

	@Autowired
	private TestService2 testService2;

	public void test1() {
	}
}