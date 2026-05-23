package com.b2chat.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the B2Chat E-Commerce Order Management System.
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
public class EcommerceOrderSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceOrderSystemApplication.class, args);
    }
}
