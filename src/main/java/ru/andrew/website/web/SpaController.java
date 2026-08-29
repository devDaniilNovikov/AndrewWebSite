package ru.andrew.website.web;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {"/", "/error"}, produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> serveIndexHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: monospace; font-size: 16px;'>");
        html.append("<h2>Диагностика: файлы в папке static</h2><ul>");
        
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            // Сканируем все файлы в папке static
            Resource[] resources = resolver.getResources("classpath:/static/*");
            for (Resource r : resources) {
                try {
                    long size = r.contentLength();
                    html.append("<li><b>").append(r.getFilename()).append("</b> &mdash; ").append(size).append(" байт</li>");
                } catch (Exception ex) {
                    html.append("<li><b>").append(r.getFilename()).append("</b> &mdash; (папка)</li>");
                }
            }
        } catch (Exception e) {
            html.append("<li>Ошибка: ").append(e.getMessage()).append("</li>");
        }
        
        html.append("</ul></body></html>");
        return ResponseEntity.ok(html.toString());
    }
}
