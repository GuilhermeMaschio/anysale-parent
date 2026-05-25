package com.anysale.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.anysale")
public class IngestionGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(IngestionGatewayApplication.class, args);
    }
}
