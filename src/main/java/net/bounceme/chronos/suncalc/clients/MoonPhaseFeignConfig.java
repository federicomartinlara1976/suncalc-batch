package net.bounceme.chronos.suncalc.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.RequestInterceptor;

public class MoonPhaseFeignConfig {

    @Value("${application.moonPhase.token}")
    private String token;

    @Bean
    RequestInterceptor moonPhaseTokenInterceptor() {
        return template -> template.query("token", token);
    }
}
