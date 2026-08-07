package com.example.todayEng.global.config;

import com.example.todayEng.domain.diary.config.AudioStorageProperties;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "storage.audio.type", havingValue = "local", matchIfMissing = true)
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
        registry.addResourceHandler(properties.publicUrlPrefix() + "/**")
                .addResourceLocations(location);
    }
}
