package org.springframework.zhao.transaction.demo01;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
public class UserServiceImpl implements UserService {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private TransactionTemplate transactionTemplate;

	/**
	 * 模拟业务操作1:先删除，再添加
	 *
	 * @author : HeHaoZhao
	 */
	@Override
	public void deleteAndSave() {
		this.transactionTemplate.executeWithoutResult(transactionStatus -> {
			//先删除表数据
			jdbcTemplate.update("delete from user");
			//调用bus2
			this.save();
		});
	}

	/**
	 * 模拟业务操作2:添加
	 *
	 * @author : HeHaoZhao
	 */
	@Override
	public void save() {
		this.transactionTemplate.executeWithoutResult(transactionStatus -> {
			jdbcTemplate.update("insert into user (name) VALUE (?)", "java");
			jdbcTemplate.update("insert into user (name) VALUE (?)", "spring");
			jdbcTemplate.update("insert into user (name) VALUE (?)", "myBatis");
		});
	}

	/**
	 * 查询表中所有数据
	 *
	 * @author : HeHaoZhao
	 */
	@Override
	public List<User> userList() {
		return jdbcTemplate.query("select * from user", new Object[]{}, new BeanPropertyRowMapper<User>(User.class));
	}
}