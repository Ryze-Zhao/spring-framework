package org.springframework.zhao.aop.demo01;


import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.zhao.aop.demo01.service.AopDemoService;
import org.springframework.zhao.aop.demo01.service.AopDemoServiceImpl;
import org.springframework.zhao.aop.demo01.service.SayNameService;
import org.springframework.zhao.aop.demo01.service.SayNameServiceImpl;

/**
 * Spring AOP 1代
 *
 * @author : HeHaoZhao
 */
public class SpringAop01Demo {
    public static void main(String[] args) {
	    SayNameService sayName = new SayNameServiceImpl();
	    // IntroductionInfo接口的内置实现
	    DelegatingIntroductionInterceptor interceptor =new DelegatingIntroductionInterceptor(sayName);
	    Advisor advisor = new DefaultIntroductionAdvisor(interceptor, SayNameService.class);

	    AopDemoService aopDemoService = new AopDemoServiceImpl();
	    ProxyFactory proxyFactory = new ProxyFactory(aopDemoService);
	    proxyFactory.addAdvisor(advisor);
	    // hello world
	    AopDemoService proxyService = (AopDemoService) proxyFactory.getProxy();
	    System.out.println(proxyService.query("hello world"));
	    // I am service
	    SayNameService proxySayName = (SayNameService) proxyFactory.getProxy();
	    System.out.println(proxySayName.getName());
    }
}
