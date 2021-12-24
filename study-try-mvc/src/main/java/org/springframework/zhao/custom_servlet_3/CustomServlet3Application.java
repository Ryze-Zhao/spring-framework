package org.springframework.zhao.custom_servlet_3;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.Tomcat;

/**
 * .
 * 功能描述:自实现servlet 3.0 规范
 *
 * 该demo若需要启动时，其他demo需要删除，原因有多个实现WebApplicationInitializer的onStartup文件，这里会报错
 *
 * @author : HeHaoZhao
 */
public class CustomServlet3Application {
    public static void main(String[] args) throws Exception {
        // 内置tomcat
        Tomcat tomcat=new Tomcat();
        tomcat.setPort(8080);
        Context context=tomcat.addContext("/",System.getProperty("java.io.tmpdir"));
	    Class<?> lifecycleListenerClass = Class.forName(tomcat.getHost().getConfigClass());
	    LifecycleListener lifecycleListener = (LifecycleListener)lifecycleListenerClass.getDeclaredConstructor().newInstance();
	    // 注册listener
        context.addLifecycleListener(lifecycleListener);
        tomcat.start();
        tomcat.getServer().await();
    }
}