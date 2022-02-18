package org.springframework.zhao.qualifier.analyze;

public class English implements Language{
	private String name;

	public English(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "English{" +
				"name='" + name + '\'' +
				'}';
	}
}
