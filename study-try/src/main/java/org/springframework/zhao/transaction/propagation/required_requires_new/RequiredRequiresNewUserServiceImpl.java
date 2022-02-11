package org.springframework.zhao.transaction.propagation.required_requires_new;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : HeHaoZhao
 */
@Component
public class RequiredRequiresNewUserServiceImpl implements RequiredRequiresNewUserService {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	/**
	 * 自调用导致事务失效问题
	 */
	@Autowired
	private RequiredRequiresNewUserService requiredRequiresNewUserService;

	@Override
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
	public void save() {
		jdbcTemplate.update("insert into user (name) VALUE (?)", "spring");
		try {
			requiredRequiresNewUserService.save2();
		} catch (Exception e) {

		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void save2() {
		jdbcTemplate.update("insert into user (name) VALUE (?)", "java");
		System.out.println(10 / 0);
	}
}
