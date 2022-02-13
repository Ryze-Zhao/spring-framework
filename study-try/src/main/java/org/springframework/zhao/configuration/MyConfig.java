package org.springframework.zhao.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.zhao.configuration.annotation.MyEnableAnnotation;
import org.springframework.zhao.configuration.pojo.Bean1;
import org.springframework.zhao.configuration.pojo.Bean2;
import org.springframework.zhao.configuration.pojo.ImportBean1;

@Configuration
@Import({ImportBean1.class, MyImportSelector.class, MyImportBeanDefinitionRegistrar.class})
@ComponentScan("org.springframework.zhao.configuration")
@ImportResource("classpath:spring/configuration/spring.xml")
@PropertySource("classpath:spring/configuration/my.properties")
@MyEnableAnnotation()
public class MyConfig {
    @Autowired
	Environment environment;

    @Bean
    public Bean1 getBean1() {
        Bean1 bean1 = new Bean1();
        bean1.setAge(Integer.valueOf(environment.getProperty("age")));
        bean1.setName(environment.getProperty("name"));
        return bean1;
    }
	//内部类
    private class InnerClass{
        @Bean
        public Bean2 getBean2(){
            return new Bean2();
        }
    }
}
