package org.springframework.zhao.error_example.newly_added;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorController {

	@RequestMapping("/err")
	public String error(){
		int i=1/0;
		return "xxx";
	}
}