package org.springframework.zhao.aop.demo05;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Aspect
//@Order(50)
public class ZhaoAspect {

	@Pointcut(value = "execution(* set*(..))")
	public void myPointCut() {
	}


	@Around("myPointCut()")
	public void myPointCutAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		System.out.println("myPointCut:::::Around1-----------");
		proceedingJoinPoint.proceed();
		System.out.println("myPointCut:::::Around2-----------");
	}

	@Before("myPointCut()")
	public void myPointCutBefore() {
		System.out.println("myPointCut:::::before-----------");
	}

	@After("myPointCut()")
	public void myPointCutAfter() {
		System.out.println("myPointCut:::::After-----------");
	}

	@AfterReturning("myPointCut()")
	public void myPointCutAfterReturning() {
		System.out.println("myPointCut:::::AfterReturning-----------");
	}

	@AfterThrowing("myPointCut()")
	public void myPointCutAfterThrowing() {
		System.out.println("myPointCut:::::AfterThrowing-----------");
	}
}