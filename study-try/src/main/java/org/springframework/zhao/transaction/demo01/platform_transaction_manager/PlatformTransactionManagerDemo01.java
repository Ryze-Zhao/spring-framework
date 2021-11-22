package org.springframework.zhao.transaction.demo01.platform_transaction_manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 *
 * @author : HeHaoZhao
 */
public class PlatformTransactionManagerDemo01 {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DataSourceConfig.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
		PlatformTransactionManager platformTransactionManager = context.getBean(PlatformTransactionManager.class);
		TransactionDefinition transactionDefinition = context.getBean(TransactionDefinition.class);

		 // 3.开启事务：调用platformTransactionManager.getTransaction开启事务操作，得到事务状态(TransactionStatus)对象
		TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);

		// 4.执行业务操作
		try {
			System.out.println("before:" + jdbcTemplate.queryForList("SELECT * from user"));
			jdbcTemplate.update("insert into user (name) values (?)", "PlatformTransactionManager-1");
			jdbcTemplate.update("insert into user (name) values (?)", "PlatformTransactionManager-2");
			// 5.提交事务：platformTransactionManager.commit
			platformTransactionManager.commit(transactionStatus);
		} catch (Exception e) {
			// 6.回滚事务：platformTransactionManager.rollback
			platformTransactionManager.rollback(transactionStatus);
		}
		System.out.println("after:" + jdbcTemplate.queryForList("SELECT * from user"));
	}
}
