package com.adherelens.clinical.config;

import com.adherelens.auth.JwtAuthFilter;
import com.adherelens.auth.JwtVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthFilter jwtAuthFilter(@Value("${adherelens.jwt.secret}") String secret) {
        return new JwtAuthFilter(new JwtVerifier(secret));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // First matching rule wins, so the specific rules come before the general ones.
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/patients/*/dose-suggestions").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/drug-rules/**").hasAnyRole("HOSPITAL_ADMIN", "DOCTOR", "NURSE")
                        .requestMatchers(HttpMethod.POST, "/api/drug-rules/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/dose-slots/*/record").hasAnyRole("NURSE", "DOCTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/regimens/*").hasAnyRole("NURSE", "DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/dose-slots/**", "/api/patients/**", "/api/regimens/**")
                                .hasAnyRole("HOSPITAL_ADMIN", "DOCTOR", "NURSE")
                        .requestMatchers(HttpMethod.POST, "/api/patients/*/regimens").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/regimens/**").hasRole("DOCTOR")
                        .requestMatchers("/api/patients/**").hasAnyRole("HOSPITAL_ADMIN", "DOCTOR")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
