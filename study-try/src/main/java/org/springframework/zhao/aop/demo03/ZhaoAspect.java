package org.springframework.zhao.aop.demo03;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ZhaoAspect {

    //    The execution of any method with a name that begins with set(名称以set开头的任何方法的执行)
    @Pointcut(value = "execution(* set*(..))")
    public void theSecondCut() {
    }


	@Around("org.springframework.zhao.aop.demo03.ZhaoAspect.theSecondCut()")
	public void theSecondCutAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		System.out.println("TheSecondCut:::::Around1-----------(名称以set开头的任何方法的执行)-----------（针对setQuery()）");
		 proceedingJoinPoint.proceed();
		System.out.println("TheSecondCut:::::Around2-----------(名称以set开头的任何方法的执行)-----------（针对setQuery()）");
	}

	@Before("org.springframework.zhao.aop.demo03.ZhaoAspect.theSecondCut()")
	public void theSecondCutBefore() {
		System.out.println("TheSecondCut:::::before-----------(名称以set开头的任何方法的执行)-----------（针对setQuery()）");
	}

	@After("org.springframework.zhao.aop.demo03.ZhaoAspect.theSecondCut()")
	public void theSecondCutAfter() {
		System.out.println("TheSecondCut:::::After-----------(名称以set开头的任何方法的执行)-----------（针对setQuery()）");
	}

}