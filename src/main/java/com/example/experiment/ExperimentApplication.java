package com.example.experiment;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class ExperimentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExperimentApplication.class, args);
	}

	@Bean
	public WebClient getWebClient(){
		return WebClient.create();
	}
}
