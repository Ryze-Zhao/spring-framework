package org.springframework.zhao.error_example;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@EnableWebMvc
@ComponentScan("org.springframework.zhao.error_example")
public class WebConfig implements WebMvcConfigurer {

}