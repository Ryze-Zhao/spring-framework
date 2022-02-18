package org.springframework.zhao.qualifier.analyze;

public class Chinese implements Language{
	private String name;

	public Chinese(String name) {
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
		return "Chinese{" +
				"name='" + name + '\'' +
				'}';
	}
}
