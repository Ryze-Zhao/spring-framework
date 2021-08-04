package spring.start.bean

import org.springframework.zhao.start.config.ZhaoConfig
import org.springframework.zhao.start.service.CityServiceImpl

beans {
	zhaoConfig(ZhaoConfig) {
	}
	cityServiceImpl(CityServiceImpl) {
		// 如果需要注入属性可以这些注入。
		// cityName = "Beijing"
	}
}