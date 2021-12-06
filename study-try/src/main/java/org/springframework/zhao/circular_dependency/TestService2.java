package org.springframework.zhao.circular_dependency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestService2 {

	@Autowired
	private TestService1 testService1;

	public void test2() {
	}
}