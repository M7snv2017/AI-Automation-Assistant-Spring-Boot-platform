package com.aiautomation;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AiAutomationApplication {

    @PostConstruct
    public void init() {
        String tz = System.getenv("TZ");
        if (tz != null && !tz.isBlank()) {
            TimeZone.setDefault(TimeZone.getTimeZone(tz));
        } else {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh"));
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(AiAutomationApplication.class, args);
    }
}
