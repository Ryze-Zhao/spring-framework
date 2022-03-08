package org.springframework.zhao.bean_name_url_handler_mapping.newly_added;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyAutoConfiguration {

	@Bean("/impl")
	public MyControllerImpl myControllerImpl(){
		return new MyControllerImpl();
	}

	@Bean("/my_handler")
	public MyControllerResquestHandler myControllerResquestHandler(){
		return new MyControllerResquestHandler();
	}
}
