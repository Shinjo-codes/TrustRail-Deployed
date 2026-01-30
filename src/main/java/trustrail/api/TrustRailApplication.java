package trustrail.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.scheduling.annotation.EnableScheduling;


import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * TRUSTRAIL MAIN APPLICATION
 *
 * Trust orchestration platform built on PayWithAccount
 * Manages instalment payments, subscriptions, and trust scoring
 */
@SpringBootApplication
public class TrustRailApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrustRailApplication.class, args);
    }

    /**
     * WebClient bean for making HTTP requests to PWA
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
