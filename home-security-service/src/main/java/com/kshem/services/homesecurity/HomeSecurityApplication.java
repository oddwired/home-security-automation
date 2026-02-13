package com.kshem.services.homesecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomeSecurityApplication {

  public static void main(String[] args) {
    SpringApplication.run(HomeSecurityApplication.class, args);
  }

}
