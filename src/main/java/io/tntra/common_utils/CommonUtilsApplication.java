package io.tntra.common_utils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CommonUtilsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommonUtilsApplication.class, args);
	}

}
