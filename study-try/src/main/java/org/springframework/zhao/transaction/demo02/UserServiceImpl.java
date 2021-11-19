package org.springframework.zhao.transaction.demo02;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
public class UserServiceImpl implements UserService {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	/**
	 * 自调用导致事务失效问题
	 * @author : HeHaoZhao
	 */
	@Autowired
	private UserService userService;


	/**
	 * 模拟业务操作1:先删除，再添加
	 *
	 * @author : HeHaoZhao
	 */
	@Override
	@Transactional
	public void deleteAndSave() {
		//先删除表数据
		jdbcTemplate.update("delete from user");
		//调用save,自调用导致事务失效问题
		userService.save();
	}

	/**
	 * 模拟业务操作2:添加
	 *
	 * @author : HeHaoZhao
	 */
	@Override
	@Transactional
	public void save() {
		jdbcTemplate.update("insert into user (name) VALUE (?)", "java");
		jdbcTemplate.update("insert into user (name) VALUE (?)", "spring");
		jdbcTemplate.update("insert into user (name) VALUE (?)", "myBatis");
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