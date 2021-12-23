package org.springframework.zhao.custom_servlet_3;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public interface CustomWebApplicationInitializer {
    void onStartup(ServletContext servletContext) throws ServletException;
}