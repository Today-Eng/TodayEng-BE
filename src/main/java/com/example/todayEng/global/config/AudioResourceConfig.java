package com.example.todayEng.global.config;

import com.example.todayEng.domain.diary.config.AudioStorageProperties;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AudioStorageProperties.class)
public class AudioResourceConfig implements WebMvcConfigurer {

    private final AudioStorageProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.directory())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/files/audio/**")
                .addResourceLocations(location);
    }
}
