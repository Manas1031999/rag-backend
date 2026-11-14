package com.manas.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Rag rag) {

    public record Jwt(String secret, long expiryMinutes) {
    }

    public record Rag(int chunkWords, int chunkOverlapWords, int topK, double similarityThreshold) {
    }
}
