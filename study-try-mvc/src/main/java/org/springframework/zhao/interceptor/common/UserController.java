package org.springframework.zhao.interceptor.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

	@GetMapping("/user")
	public User getUser() {
		return new User("getUser:RyzeZhao", 18);
	}

	@PostMapping("/user")
	public User postUser() {
		return new User("postUser:RyzeZhao", 19);
	}

	@RequestMapping(value={"/user","/user1"})
	public User userORUser1() {
		return new User("userORUser1:RyzeZhao", 20);
	}
}