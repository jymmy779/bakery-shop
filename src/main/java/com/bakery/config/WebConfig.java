package com.bakery.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final Path uploadPath;

  public WebConfig(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
    this.uploadPath = Path.of(uploadDirectory).toAbsolutePath().normalize();
  }

  @PostConstruct
  void createUploadDirectory() throws IOException {
    Files.createDirectories(uploadPath);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/uploads/**")
        .addResourceLocations(uploadPath.toUri().toString());
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/products").setViewName("product/list");
    registry.addViewController("/products/{productId}").setViewName("product/detail");
    registry.addViewController("/about").setViewName("about/index");
    registry.addViewController("/contact").setViewName("contact/index");
  }
}
