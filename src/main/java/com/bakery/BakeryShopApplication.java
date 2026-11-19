package com.bakery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BakeryShopApplication {

  public static void main(String[] args) {
    SpringApplication.run(BakeryShopApplication.class, args);
  }

}

