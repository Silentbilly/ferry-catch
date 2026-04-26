package com.ferrycatch.api.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ferrycatch.snapshot")
public record SnapshotProperties(
        String prodBaseUrl,
        String date,
        String outputDir
) {}
