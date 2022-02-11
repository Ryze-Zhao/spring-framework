package org.springframework.zhao.transaction.propagation;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.zhao.transaction.propagation.required_nested.RequiredNestedUserService;

/**
 *
 * @author : RyzeZhao
 */
public class PropagationMain {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DataSourceConfig.class);
		// 内外都是REQUIRED
//		RequiredRequiredUserService requiredRequiredUserService = context.getBean(RequiredRequiredUserService.class);
//		requiredRequiredUserService.save();

		// 外REQUIRED，内REQUIRES_NEW
//		RequiredRequiresNewUserService requiredRequiresNewUserService = context.getBean(RequiredRequiresNewUserService.class);
//		requiredRequiresNewUserService.save();

		// 外REQUIRED，内层Nested
		RequiredNestedUserService requiredNestedUserService = context.getBean(RequiredNestedUserService.class);
		requiredNestedUserService.save();
	}
}
