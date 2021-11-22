package org.springframework.zhao.transaction.demo02;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 *
 * @author : HeHaoZhao
 */
@Component
public class UserServiceImpl implements UserService {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	/**
	 * 自调用导致事务失效问题
	 */
	@Autowired
	private UserService userService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteAndSave() {
		//先删除表数据
		jdbcTemplate.update("delete from user");
		//调用save,自调用导致事务失效问题
		userService.save();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void save() {
		jdbcTemplate.update("insert into user (name) VALUE (?)", "java");
		jdbcTemplate.update("insert into user (name) VALUE (?)", "spring");
		jdbcTemplate.update("insert into user (name) VALUE (?)", "myBatis");
	}

	@Override
	public List<User> userList() {
		return jdbcTemplate.query("select * from user", new Object[]{}, new BeanPropertyRowMapper<User>(User.class));
	}
}