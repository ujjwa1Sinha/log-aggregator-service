package com.dstrLogAggr.Log_Aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class LogAggregatorApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(LogAggregatorApplication.class)
				.properties("server.address=0.0.0.0")
				.run(args);
	}

}
