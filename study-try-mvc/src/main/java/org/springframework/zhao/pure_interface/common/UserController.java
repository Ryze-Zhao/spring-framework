package org.springframework.zhao.pure_interface.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

	@GetMapping("/user")
	public User getUser() {
		return new User("RyzeZhao", 18);
	}
}