package com.codeops.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a {@link RestTemplate} bean configured with connection and read timeouts
 * for outbound HTTP requests (webhook notifications, Registry health checks, external API calls).
 *
 * @see com.codeops.notification.TeamsWebhookService
 * @see com.codeops.registry.service.ServiceRegistryService
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a {@link RestTemplate} with a 5-second connect timeout and 10-second read timeout.
     *
     * @return the configured {@link RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return new RestTemplate(factory);
    }
}
