package org.springframework.zhao.transaction.demo02;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 *
 * @author : RyzeZhao
 */
public class TransactionDemo02 {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DataSourceConfig.class);
		UserService userService = context.getBean(UserService.class);
		userService.deleteAndSave();
//		System.out.println(userService.userList());
	}
}
