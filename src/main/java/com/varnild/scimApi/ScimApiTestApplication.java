package com.varnild.scimApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ScimApiTestApplication {

	private static final Logger log = LoggerFactory.getLogger(ScimApiTestApplication.class);
	
	public static void main(String[] args) {
		ConfigurableApplicationContext app = SpringApplication.run(ScimApiTestApplication.class, args);
		log.info("SCIM api is running at http://localhost:8088/scim/");
		log.info("Service is running at http://localhost:8088/service/");
	}

}
