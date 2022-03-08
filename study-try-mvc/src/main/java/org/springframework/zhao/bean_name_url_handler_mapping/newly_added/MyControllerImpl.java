package org.springframework.zhao.bean_name_url_handler_mapping.newly_added;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyControllerImpl implements Controller {
	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		System.out.println("MyControllerImpl");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("MyControllerImpl");
		modelAndView.addObject("msg", "MyControllerImpl");
		return modelAndView;
	}
}
