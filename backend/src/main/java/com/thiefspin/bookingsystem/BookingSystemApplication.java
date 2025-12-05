package com.thiefspin.bookingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BookingSystemApplication {

  public static void main(String[] args) {
    SpringApplication.run(BookingSystemApplication.class, args);
  }

}
