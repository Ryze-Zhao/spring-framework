package org.springframework.zhao.transaction.demo01.transaction_template;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

/**
 * @author : HeHaoZhao
 */
public class TransactionTemplateDemo01 {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DataSourceConfig.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
		TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);

		/*
		 * 4.通过TransactionTemplate提供的方法执行业务操作
		 * 4.1.1.主要有2个方法：
		 * 4.1.1.1.executeWithoutResult(Consumer<TransactionStatus> action)：没有返回值的，需传递一个Consumer对象，在accept方法中做业务操作
		 * 4.1.1.2.<T> T execute(TransactionCallback<T> action)：有返回值的，需要传递一个TransactionCallback对象，在doInTransaction方法中做业务操作
		 * 调用execute方法或者executeWithoutResult方法执行完毕之后，事务管理器会自动提交事务或者回滚事务。
		 *
		 * 4.1.2.那么什么时候事务会回滚，有2种方式：
		 * 4.1.2.1.transactionStatus.setRollbackOnly();将事务状态标注为回滚状态
		 * 4.1.2.2.execute方法或者executeWithoutResult方法内部抛出异常
		 *
		 * 4.1.3.什么时候事务会提交？
		 * 方法没有异常 && 未调用过transactionStatus.setRollbackOnly();
		 */
		System.out.println("before:" + jdbcTemplate.queryForList("SELECT * from user"));
		transactionTemplate.executeWithoutResult(new Consumer<TransactionStatus>() {
			@Override
			public void accept(TransactionStatus transactionStatus) {
				jdbcTemplate.update("insert into user (name) values (?)", "transactionTemplate-1");
				jdbcTemplate.update("insert into user (name) values (?)", "transactionTemplate-2");

			}
		});
		System.out.println("after:" + jdbcTemplate.queryForList("SELECT * from user"));
	}
}
