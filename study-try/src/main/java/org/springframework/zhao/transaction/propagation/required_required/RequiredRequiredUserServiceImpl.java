package org.springframework.zhao.transaction.propagation.required_required;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : HeHaoZhao
 */
@Component
public class RequiredRequiredUserServiceImpl implements RequiredRequiredUserService {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	/**
	 * 自调用导致事务失效问题
	 */
	@Autowired
	private RequiredRequiredUserService requiredRequiredUserService;

	@Override
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
	public void save() {
		jdbcTemplate.update("insert into user (name) VALUE (?)", "spring");
		requiredRequiredUserService.save2();
	}

	@Override
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
	public void save2() {
		jdbcTemplate.update("insert into user (name) VALUE (?)", "java");
//		System.out.println(10 / 0);
	}
}