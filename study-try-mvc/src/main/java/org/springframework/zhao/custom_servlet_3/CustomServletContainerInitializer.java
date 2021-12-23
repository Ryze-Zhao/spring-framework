package org.springframework.zhao.custom_servlet_3;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.ReflectionUtils;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

// 提供ServletContainerInitializer接口实现类，加上@HandlesTypes注解，传入自己提供的CustomWebApplicationInitializer接口
@HandlesTypes(CustomWebApplicationInitializer.class)
public class CustomServletContainerInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        List<CustomWebApplicationInitializer> initializers = new LinkedList<>();
        if (c != null) {
            for (Class<?> waiClass : c) {
                if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) && CustomWebApplicationInitializer.class.isAssignableFrom(waiClass)) {
                    try {
                        initializers.add((CustomWebApplicationInitializer) ReflectionUtils.accessibleConstructor(waiClass).newInstance());
                    } catch (Throwable ex) {
                        throw new ServletException("Failed to instantiate CustomWebApplicationInitializer class", ex);
                    }
                }
            }
        }
        if (initializers.isEmpty()) {
            ctx.log("No Spring CustomWebApplicationInitializer types detected on classpath");
            return;
        }
        ctx.log(initializers.size() + " Spring CustomWebApplicationInitializers detected on classpath");
        AnnotationAwareOrderComparator.sort(initializers);
        for (CustomWebApplicationInitializer initializer : initializers) {
            initializer.onStartup(ctx);
        }
    }
}
