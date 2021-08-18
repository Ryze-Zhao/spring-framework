package org.springframework.zhao.life_cycle;


import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.zhao.initializing_bean.MyInitializingBean;


public class LifeCycleTest {
    public static void main(String[] args) {
	    // 容器初始化阶段,保存 BeanDefinition
	    ClassPathResource resource = new ClassPathResource("spring/life_cycle/bean/SpringConfig.xml");
	    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
	    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
	    reader.loadBeanDefinitions(resource);


	    // BeanFactory 容器一定要调用该方法进行 BeanPostProcessor 注册
	    factory.addBeanPostProcessor(new LifeCycleBean());
	    LifeCycleBean lifeCycleBean =factory.getBean(LifeCycleBean.class);
	    lifeCycleBean.display();
	    System.out.println("方法调用完成，容器开始关闭....");
		// 关闭容器
	    factory.destroySingletons();
    }
}
