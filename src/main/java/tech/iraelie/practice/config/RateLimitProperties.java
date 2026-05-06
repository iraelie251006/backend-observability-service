package tech.iraelie.practice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "spring.rate-limit")
@Configuration
@Getter @Setter
public class RateLimitProperties {
    private int capacity;
    private int refillTokens;
    private int refillSeconds;
    private List<String> protectedPaths;
}
