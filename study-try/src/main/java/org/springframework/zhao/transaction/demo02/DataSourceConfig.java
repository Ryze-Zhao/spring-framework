package org.springframework.zhao.transaction.demo02;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@ComponentScan("org.springframework.zhao.transaction.demo02")
@EnableTransactionManagement
public class DataSourceConfig {

	/**
	 * 定义一个数据源
	 * @author : HeHaoZhao
	 */
	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://localhost:3306/spring?characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC");
		dataSource.setUsername("root");
		dataSource.setPassword("mysql123");
		return dataSource;
	}


	/**
	 * 定义一个JdbcTemplate，用来方便执行数据库增删改查
	 * @author : HeHaoZhao
	 */
	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}


	/**
	 * 1.定义事务管理器，给其指定一个数据源（可以把事务管理器想象为一个人，这个人来负责事务的控制操作）
	 * @author : HeHaoZhao
	 */
	@Bean
	public PlatformTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}
}
