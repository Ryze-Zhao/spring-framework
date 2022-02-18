package org.springframework.zhao.qualifier.analyze;

public class Russian implements Language {
	private String name;

	public Russian(String name) {
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
		return "Russian{" +
				"name='" + name + '\'' +
				'}';
	}
}
