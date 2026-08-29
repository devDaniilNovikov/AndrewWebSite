package ru.andrew.website.web;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;

@Controller
public class SpaController implements ErrorController {

    @RequestMapping(value = {"/", "/error"})
    public ResponseEntity<String> serveIndexHtml() {
        try {
            Resource resource = new ClassPathResource("static/index.html");
            if (resource.exists()) {
                // Если файл есть — читаем его вручную и отдаем браузеру
                String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
            } else {
                // Если файла нет, выводим ошибку, чтобы больше не видеть белый экран!
                return ResponseEntity.status(404)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<h1>Критическая ошибка сборки</h1>" +
                              "<p>Файл <b>index.html</b> физически отсутствует внутри Java-архива.</p>" +
                              "<p>Это значит, что Dockerfile не смог скопировать фронтенд в папку static.</p>");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка чтения файла: " + e.getMessage());
        }
    }
}
