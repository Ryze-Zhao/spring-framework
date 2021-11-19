package org.springframework.zhao.transaction.demo01;

import java.util.List;

public interface UserService {
	void deleteAndSave();
	void save();
	List<User> userList();
}
