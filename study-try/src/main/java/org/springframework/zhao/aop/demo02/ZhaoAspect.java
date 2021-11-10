package org.springframework.zhao.aop.demo02;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ZhaoAspect {

    //    The execution of any public method(任何以public的方法执行)
    @Pointcut(value = "execution(public * *(..))")
    public void theFirstCut() {
    }

    //    The execution of any method with a name that begins with set(名称以set开头的任何方法的执行)
    @Pointcut(value = "execution(* set*(..))")
    public void theSecondCut() {
    }

    //    The execution of any method defined by the AccountService interface(AccountService接口定义的任何方法的执行)
    @Pointcut(value = "execution(* org.springframework.zhao.aop.demo02.service.AccountService..*(..))")
    public void theThirdCut() {
    }

    //    The execution of any method defined in the service package(service包中定义的任何方法的执行)
    @Pointcut(value = "execution(* org.springframework.zhao.aop.demo02.service.*.*(..))")
    public void theFourthCut() {
    }

    //    The execution of any method defined in the service package or one of its sub-packages(服务包或其子包之一中定义的任何方法的执行)
    @Pointcut(value = "execution(* org.springframework.zhao.aop.demo02.service..*.*(..))")
    public void theFifthCut() {
    }

    //    Any join point (method execution only in Spring AOP) within the service package(服务包中的任何连接点（仅在Spring AOP中执行方法）)
    @Pointcut(value = "within(org.springframework.zhao.aop.demo02.service.*)")
    public void theSixthCut() {
    }

    //    Any join point (method execution only in Spring AOP) within the service package or one of its sub-packages(服务包或其子包之一中的任何连接点（仅在Spring AOP中执行方法）)
    @Pointcut(value = "within(org.springframework.zhao.aop.demo02.service..*)")
    public void theSeventhCut() {
    }

    //    Any join point (method execution only in Spring AOP) where the proxy implements the AccountService interface(代理实现AccountService接口的任何连接点（仅在Spring AOP中是方法执行）)
    @Pointcut(value = "this(org.springframework.zhao.aop.demo02.service.AccountService)")
    public void theEighthCut() {
    }

    //    Any join point (method execution only in Spring AOP) where the target object implements the AccountService interface(目标对象实现AccountService接口的任何连接点（仅在Spring AOP中执行方法）)
    @Pointcut(value = "target(org.springframework.zhao.aop.demo02.service.AccountService)")
    public void theNinthCut() {
    }

    //    Any join point (method execution only in Spring AOP) that takes a single parameter and where the argument passed at runtime is Serializable(任何采用任意参数且运行时传递的参数为的连接点（仅在Spring AOP中是方法执行）)
    //该切点表达式将匹配第一个参数为java.lang.String，最后一个参数为java.lang.Integer，并且中间可以有任意个数和类型参数的方法：
    @Pointcut(value = "args(java.lang.String,..,java.lang.Integer)")
    public void theTenthCut() {
    }

    //代表使用了@MyAnnotation注解的类
    @Pointcut(value = "@within(org.springframework.zhao.aop.demo02.MyAnnotation)")
    public void theEleventhCut() {

    }
    //代表使用了@MyAnnotation注解的方法
    @Pointcut(value = "@annotation(org.springframework.zhao.aop.demo02.MyAnnotation)")
    public void theTwelfthCut() {

    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theFirstCut()")
    public void theFirstCutBefore() {
        System.out.println("TheFirstCut:::::before-----------(任何以public的方法执行)-----------（针对query()、query1(String aa,int bb)、setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theSecondCut()")
    public void theSecondCutBefore() {
        System.out.println("TheSecondCut:::::before-----------(名称以set开头的任何方法的执行)-----------（针对setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theThirdCut()")
    public void theThirdCutBefore() {
        System.out.println("TheThirdCut:::::before-----------(AccountService接口定义的任何方法的执行)-----------（针对query()、query1(String aa,int bb)、setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theFourthCut()")
    public void theFourthCutBefore() {
        System.out.println("TheFourthCut:::::before-----------(service包中定义的任何方法的执行)-----------（针对query()、query1(String aa,int bb)、setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theFifthCut()")
    public void theFifthCutBefore() {
        System.out.println("TheFifthCut:::::before-----------(服务包或其子包之一中定义的任何方法的执行)-----------（针对query()、query1(String aa,int bb)、setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theSixthCut()")
    public void theSixthCutBefore() {
        System.out.println("TheSixthCut:::::before-----------(服务包中的任何连接点（仅在Spring AOP中执行方法）-----------（针对query()、query1(String aa,int bb)、setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theEighthCut()")
    public void theSeventhCutBefore() {
        System.out.println("TheSeventhCut:::::before-----------(服务包或其子包之一中的任何连接点（仅在Spring AOP中执行方法）-----------（针对query()、query1(String aa,int bb)、setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theEighthCut()")
    public void theEighthCutBefore() {
        System.out.println("TheEighthCut:::::before-----------(代理实现AccountService接口的任何连接点（仅在Spring AOP中是方法执行）-----------（针对query()、query1(String aa,int bb)、setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theNinthCut()")
    public void theNinthCutBefore() {
        System.out.println("TheNinthCut:::::before-----------(目标对象实现AccountService接口的任何连接点（仅在Spring AOP中执行方法）-----------（针对query()、query1(String aa,int bb)、setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theTenthCut()")
    public void theTenthCutBefore() {
        System.out.println("TheTenthCut:::::before-----------(任何采用任意参数且运行时传递的参数为的连接点（仅在Spring AOP中是方法执行）)-----------（针对query1(String aa,int bb)）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theEleventhCut()")
    public void theEleventhCutBefore() {
        System.out.println("TheEleventhCut:::::before-----------(代表使用了@MyAnnotation注解的类)-----------（针对query()、query1(String aa,int bb)、setQuery()）");
    }

    @Before("org.springframework.zhao.aop.demo02.ZhaoAspect.theTwelfthCut()")
    public void theTwelfthCutBefore() {
        System.out.println("TheTwelfthCut:::::before-----------(代表使用了@MyAnnotation注解的方法)-----------（针对setQuery()）");
    }
}