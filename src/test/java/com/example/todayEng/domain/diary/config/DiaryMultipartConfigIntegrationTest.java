package com.example.todayEng.domain.diary.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.service.ImageUploadValidator;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(classes = DiaryMultipartConfigIntegrationTest.App.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiaryMultipartConfigIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void acceptsTwoSevenMegabyteImagesWithJsonPart() {
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("request", part("{}", MediaType.APPLICATION_JSON, null));
        body.add("images", image("first.png"));
        body.add("images", image("second.png"));
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var response = restTemplate.postForEntity(
                "/test/multipart", new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("2");
    }

    private HttpEntity<?> image(String filename) {
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        return part(Arrays.copyOf(png, 7 * 1024 * 1024), MediaType.IMAGE_PNG, filename);
    }

    private HttpEntity<?> part(Object content, MediaType type, String filename) {
        Object value = filename == null ? content : new ByteArrayResource((byte[]) content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        var headers = new HttpHeaders();
        headers.setContentType(type);
        return new HttpEntity<>(value, headers);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
    @Import({DiaryMultipartConfig.class, ImageUploadValidator.class, Controller.class})
    static class App {
    }

    @RestController
    static class Controller {
        private final ImageUploadValidator validator;

        Controller(ImageUploadValidator validator) {
            this.validator = validator;
        }

        @PostMapping(value = "/test/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        int upload(@RequestPart("request") String request,
                @RequestPart("images") List<MultipartFile> images) {
            return validator.validate(images).size();
        }
    }
}
