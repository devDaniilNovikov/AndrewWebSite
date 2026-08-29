package ru.andrew.website.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Указываем путь только для служебных файлов Next.js (чтобы убрать белый экран)
        registry.addResourceHandler("/_next/**")
                .addResourceLocations("classpath:/static/_next/");
    }
}
