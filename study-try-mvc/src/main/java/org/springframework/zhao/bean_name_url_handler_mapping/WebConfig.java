package org.springframework.zhao.bean_name_url_handler_mapping;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;

import java.util.List;

@Configuration
@EnableWebMvc
@ComponentScan("org.springframework.zhao.bean_name_url_handler_mapping")
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		// 配置消息转换器 这里用fastjson，可以自由选择
		FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();
		converters.add(fastJsonHttpMessageConverter);
	}


}