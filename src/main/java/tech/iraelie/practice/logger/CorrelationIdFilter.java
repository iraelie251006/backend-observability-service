package tech.iraelie.practice.logger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";
    private static final String MDC_USER = "userId";
    private static final String MDC_SERVICE = "service";
    private static final String MDC_METHOD = "httpMethod";
    private static final String MDC_PATH = "httpPath";

    private static final Pattern SAFE = Pattern.compile("^[a-zA-Z0-9\\-]{1,64}$");

    @Value("${spring.application.name}")
    private String serviceName;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String raw = request.getHeader(CORRELATION_HEADER);
            String correlationId = (raw != null && SAFE.matcher(raw).matches())
                    ? raw : UUID.randomUUID().toString();

            MDC.put(MDC_KEY, correlationId);
            MDC.put(MDC_SERVICE, serviceName);
            MDC.put(MDC_METHOD, request.getMethod());
            MDC.put(MDC_PATH, request.getRequestURI());

            extraUserId().ifPresent(id -> MDC.put(MDC_USER, id));

            response.setHeader(CORRELATION_HEADER, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private Optional<String> extraUserId() {
        return Optional.ofNullable(
                SecurityContextHolder.getContext().getAuthentication()
        ).map(Principal::getName);
    }
}
