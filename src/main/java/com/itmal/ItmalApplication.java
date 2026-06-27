package com.itmal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ItmalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItmalApplication.class, args);
    }

}
