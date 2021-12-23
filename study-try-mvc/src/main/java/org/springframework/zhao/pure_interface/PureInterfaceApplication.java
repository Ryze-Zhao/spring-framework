package org.springframework.zhao.pure_interface;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.springframework.context.annotation.ComponentScan;




public class PureInterfaceApplication {
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