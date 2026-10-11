package com.dailycode.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DailyCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyCodeApplication.class, args);
        System.out.print("\n-INIT-\n\n");
    }
}
