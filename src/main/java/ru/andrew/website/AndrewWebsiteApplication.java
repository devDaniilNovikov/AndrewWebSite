package ru.andrew.website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AndrewWebsiteApplication {
    public static void main(String[] args) {
        SpringApplication.run(AndrewWebsiteApplication.class, args);
    }
}
