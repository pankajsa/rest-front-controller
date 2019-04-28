package com.solace.samples.restcontroller;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class RestFrontControllerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestFrontControllerApplication.class, args);
	}

	@Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		
        return args -> {
            log.info("Application Started");

            
            Controller.doIt(args);
            
            
            
            
            log.info("Application Terminated");
        };
	}

	
}
