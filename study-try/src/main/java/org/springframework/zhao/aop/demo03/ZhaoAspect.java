package org.springframework.zhao.aop.demo03;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ZhaoAspect {

    //    The execution of any method with a name that begins with set(名称以set开头的任何方法的执行)
    @Pointcut(value = "execution(* set*(..))")
    public void theSecondCut() {
    }

    @Before("org.springframework.zhao.aop.demo03.ZhaoAspect.theSecondCut()")
    public void theSecondCutBefore() {
        System.out.println("TheSecondCut:::::before-----------(名称以set开头的任何方法的执行)-----------（针对setQuery()）");
    }
}