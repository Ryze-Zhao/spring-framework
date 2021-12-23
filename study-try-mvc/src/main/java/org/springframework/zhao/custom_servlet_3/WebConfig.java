package org.springframework.zhao.custom_servlet_3;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;


@EnableWebMvc
@Configuration
@ComponentScan("org.springframework.zhao.custom_servlet_3")
public class WebConfig implements WebMvcConfigurer {
	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
//		registry.jsp("/page/",".html");
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		// 配置消息转换器 这里用fastjson，可以自由选择
		FastJsonHttpMessageConverter fastJsonHttpMessageConverter=new FastJsonHttpMessageConverter();
		converters.add(fastJsonHttpMessageConverter);
	}
}