package com.psehrawa.oppfinder.discovery.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .responseTimeout(Duration.ofSeconds(30))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse())
            .filter(handleErrors())
            .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Response: {} {}", clientResponse.statusCode(), clientResponse.headers().asHttpHeaders());
            return Mono.just(clientResponse);
        });
    }

    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                log.warn("Error response: {} {}", clientResponse.statusCode(), clientResponse.headers().asHttpHeaders());
            }
            return Mono.just(clientResponse);
        });
    }

    @Bean("githubWebClient")
    public WebClient githubWebClient() {
        return webClient()
            .mutate()
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github.v3+json")
            .defaultHeader("User-Agent", "OpportunityFinder/1.0")
            .build();
    }

    @Bean("hackerNewsWebClient")
    public WebClient hackerNewsWebClient() {
        return webClient()
            .mutate()
            .baseUrl("https://hacker-news.firebaseio.com/v0")
            .defaultHeader("User-Agent", "OpportunityFinder/1.0")
            .build();
    }

    @Bean("redditWebClient")
    public WebClient redditWebClient() {
        return webClient()
            .mutate()
            .baseUrl("https://www.reddit.com")
            .defaultHeader("User-Agent", "OpportunityFinder/1.0")
            .build();
    }
}