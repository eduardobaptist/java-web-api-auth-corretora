package br.com.eduardobaptista.javaweb2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String appUploadDir;

    // serve os arquivos enviados em ${app.upload.dir} como /uploads/{imovelId}/{arquivo}
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String local = Paths.get(appUploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!local.endsWith("/"))
            local += "/";

        registry.addResourceHandler("/uploads/**").addResourceLocations(local);
    }
}
