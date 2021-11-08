package org.springframework.zhao.customize_conversion;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

public class StringToCityConversion implements Converter<String, City> {

	@Override
	public City convert(String source) {
		if (StringUtils.hasLength(source)) {
			String[] sources = source.split("#");

			City city = new City();
			city.setName(sources[0]);
			city.setCode(Integer.parseInt(sources[1]));
			return city;
		}
		return null;
	}
}