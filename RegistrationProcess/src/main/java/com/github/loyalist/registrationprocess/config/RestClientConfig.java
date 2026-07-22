package com.github.loyalist.registrationprocess.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class RestClientConfig {
    @Value("${metadata.service.url}")
    private String metadataServiceUrl;

    @Value("${storage.service.url}")
    private String storageServiceUrl;

    @Bean(name = "metadataRestClient")
    public RestClient metadataRestClient() {
        java.net.http.HttpClient jdkHttpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
        factory.setReadTimeout(Duration.ofMillis(5000));

        return RestClient.builder()
                .baseUrl(metadataServiceUrl)
                .requestFactory(factory)
                .build();
    }

    @Bean(name = "storageRestClient")
    public RestClient storageRestClient() {
        java.net.http.HttpClient jdkHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
        factory.setReadTimeout(Duration.ofMillis(5000));

        return RestClient.builder()
                .baseUrl(storageServiceUrl)
                .requestFactory(factory)
                .build();
    }
}
