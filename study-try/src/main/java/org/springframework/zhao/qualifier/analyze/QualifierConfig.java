package org.springframework.zhao.qualifier.analyze;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QualifierConfig {

	@Bean
	@CanLanguage
	public Language chinese() {
		return new Chinese("中文");
	}


	@Bean
	@CanLanguage
	public Language english() {
		return new English("英文");
	}

	@Bean
	public Language russion() {
		return new Russian("俄语");
	}

	@Bean
	public People people() {
		return new People();
	}
}

