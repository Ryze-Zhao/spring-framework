package org.springframework.zhao.bean_name_url_handler_mapping.newly_added;

import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MyControllerResquestHandler implements HttpRequestHandler {
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("MyControllerResquestHandler");
		response.getWriter().println("MyControllerResquestHandler");
	}
}
