package com.ferrycatch.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(SnapshotProperties.class)
public class SnapshotTestConfig {

    @Bean
    RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    @Bean
    RestTemplate prodRestTemplate(RestTemplateBuilder builder, SnapshotProperties props) {
        return builder.rootUri(props.prodBaseUrl()).build();
    }
}

