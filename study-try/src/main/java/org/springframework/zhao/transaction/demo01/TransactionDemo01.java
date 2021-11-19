package org.springframework.zhao.transaction.demo01;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class TransactionDemo01 {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DataSourceConfig.class);
		UserService userService = context.getBean(UserService.class);
		userService.deleteAndSave();
		System.out.println(userService.userList());
	}
}
