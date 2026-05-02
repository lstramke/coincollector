package io.github.lstramke.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import io.github.lstramke.coincollector.services.SessionManager;

/**
 * Configures the application's Spring Security chain.
 * <p>
 * This setup wires the custom session filter, defines the REST entry point
 * for unauthorized requests, and keeps the login and static routes open.
 */
@Configuration
public class SecurityConfig {
    /**
     * Creates the REST authentication entry point.
     */
    @Bean
    public RestAuthenticationEntryPoint restEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    /**
     * Creates the custom session filter.
     */
    @Bean
    public SessionFilter sessionFilter(SessionManager sessionManager) {
        return new SessionFilter(sessionManager);
    }

    /**
     * Builds the Spring Security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionFilter sessionFilter, RestAuthenticationEntryPoint entryPoint) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(entryPoint))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/login", "/api/v1/registration", "/api/v1/shutdown", "/", "/index.html", "/static/**", "/assets/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
