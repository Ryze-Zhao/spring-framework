package org.springframework.zhao.pure_interface.common;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;

@ControllerAdvice
public class DemoControllerAdvice {

	@InitBinder
	public void initBinderDateType(WebDataBinder webDataBinder) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		webDataBinder.registerCustomEditor(Date.class, new CustomDateEditor(simpleDateFormat,true));
	}

	@ModelAttribute
	public void globalAttribute(Model model){
		model.addAttribute("globalValue", "WOW~全局属性");
	}
}