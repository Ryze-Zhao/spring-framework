package org.springframework.zhao.aop.demo01;


import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.zhao.aop.demo01.service.AopDemoService;

import java.lang.reflect.Method;
import java.util.Objects;

public class AopDemoPointcut implements Pointcut {
	@Override
	public ClassFilter getClassFilter() {
		return new ClassFilter() {
			@Override
			public boolean matches(Class<?> clazz) {
				return AopDemoService.class.isAssignableFrom(clazz);
			}
		};
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return new MethodMatcher() {
			@Override
			public boolean matches(Method method, Class<?> targetClass) {
				return "echo".equals(method.getName()) &&
						method.getParameterTypes().length == 1 &&
						Objects.equals(String.class, method.getParameterTypes()[0]);
			}

			@Override
			public boolean isRuntime() {
				return false;
			}

			@Override
			public boolean matches(Method method, Class<?> targetClass, Object... args) {
				return false;
			}
		};
	}
}
