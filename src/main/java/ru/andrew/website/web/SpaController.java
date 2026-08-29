package ru.andrew.website.web;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        // Если Spring не нашел какой-то путь (например, /about), 
        // он возвращает главный файл Next.js, чтобы фронтенд сам нарисовал нужную страницу.
        return "forward:/index.html";
    }
}
