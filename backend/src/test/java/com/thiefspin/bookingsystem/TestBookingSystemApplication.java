package com.thiefspin.bookingsystem;

import org.springframework.boot.SpringApplication;

public class TestBookingSystemApplication {

  public static void main(String[] args) {
    SpringApplication.from(BookingSystemApplication::main).with(TestcontainersConfiguration.class)
        .run(args);
  }

}
