package org.springframework.zhao.initializing_bean2;

public class MyInitializingBean2 {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOtherName() {
		System.out.println("initializing_bean2.MyInitializingBean2 setOtherName...");
		this.name = "initializing_bean2.MyInitializingBean2$Name$Code";
	}
}



