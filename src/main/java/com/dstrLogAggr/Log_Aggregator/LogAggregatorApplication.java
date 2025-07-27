package com.dstrLogAggr.Log_Aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class LogAggregatorApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(LogAggregatorApplication.class)
				.properties("server.address=0.0.0.0")
				.run(args);
	}

}
