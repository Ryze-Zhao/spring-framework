package org.springframework.zhao.pure_interface.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/combine")
public class CombineController {

	@GetMapping("/demo")
	public User combine() {
		return new User("combine:RyzeZhao", 21);
	}


}