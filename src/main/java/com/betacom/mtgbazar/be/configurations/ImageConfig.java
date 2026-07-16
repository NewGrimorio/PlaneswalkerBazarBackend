package com.betacom.mtgbazar.be.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class ImageConfig implements WebMvcConfigurer{
	
	@Value("${app.upload.dir}")
    private String uploadDir;

    /** /immagini/** -> cartella esterna al jar: sopravvive ai redeploy */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/immagini/**")
                .addResourceLocations("file:" + Path.of(uploadDir).toAbsolutePath() + "/");
    }
}
