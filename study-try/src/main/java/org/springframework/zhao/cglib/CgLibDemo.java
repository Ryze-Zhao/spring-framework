package org.springframework.zhao.cglib;

import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.cglib.proxy.*;


import java.lang.reflect.Method;

public class CgLibDemo {
	public static void main(String[] args) {
		//把CGLIB生成的字节码文件保存到本地D:\cglib，到时候可以拖进idea里看
		System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "E:\\cglib");
		MethodInterceptor m1 = new MethodInterceptor() {
			@Override
			public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
				System.out.println("拦截器1 before");
				methodProxy.invokeSuper(o, objects);
				System.out.println("拦截器1 after");
				return null;
			}
		};
		MethodInterceptor m2 = new MethodInterceptor() {
			@Override
			public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
				System.out.println("拦截器2 before");
				methodProxy.invokeSuper(o, objects);
				System.out.println("拦截器2 after");
				return null;
			}
		};
		//返回索引对应Callback数组里的拦截器索引
		CallbackFilter callbackFilter = method -> {
			if (method.getName().equals("func1")) {
				return 0;
			} else if (method.getName().equals("func2")) {
				return 1;
			}
			return 2;
		};

		CglibObj cglibObj1 = (CglibObj) Enhancer.create(CglibObj.class, null, callbackFilter, new Callback[]{m1, m2, NoOp.INSTANCE});
		cglibObj1.func1();
		cglibObj1.func1();
		cglibObj1.func2();
		CglibObj cglibObj2 = (CglibObj) Enhancer.create(CglibObj.class, null, callbackFilter, new Callback[]{m1, m2, NoOp.INSTANCE});
		cglibObj2.func1();
		cglibObj2.func2();
	}
}


