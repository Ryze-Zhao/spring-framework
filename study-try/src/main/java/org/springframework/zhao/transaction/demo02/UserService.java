package org.springframework.zhao.transaction.demo02;

import java.util.List;

/**
 * 模拟业务类
 * @author : HeHaoZhao
 */
public interface UserService {
	/**
	 * 模拟业务操作1:先删除，再添
	 */
	void deleteAndSave();
	/**
	 * 模拟业务操作2:添加
	 */
	void save();
	/**
	 * 查询表中所有数据
	 */
	List<User> userList();
}
