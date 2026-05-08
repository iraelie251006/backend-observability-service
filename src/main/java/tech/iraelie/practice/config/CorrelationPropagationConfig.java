package tech.iraelie.practice.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CorrelationPropagationConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter((request, next) -> {
                    String cid = MDC.get("correlationId");
                    ClientRequest mutated = (cid != null)
                            ? ClientRequest.from(request)
                            .header("X-Correlation-ID", cid)
                            .build()
                            : request;
                    return next.exchange(mutated);
                })
                .build();
    }
}
