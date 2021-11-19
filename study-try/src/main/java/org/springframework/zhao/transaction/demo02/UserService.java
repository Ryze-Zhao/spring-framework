package org.springframework.zhao.transaction.demo02;

import java.util.List;

public interface UserService {
	void deleteAndSave();
	void save();
	List<User> userList();
}
