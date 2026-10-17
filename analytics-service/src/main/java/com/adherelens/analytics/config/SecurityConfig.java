package com.adherelens.analytics.config;

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
                // First matching rule wins. "/error" must be open or a 403 shows up as 401.
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/error").permitAll()
                        // Own hospital's numbers.
                        .requestMatchers(HttpMethod.GET, "/api/analytics/hospital/**").hasAnyRole("HOSPITAL_ADMIN", "DOCTOR")
                        // All hospitals together, with hospital names hidden.
                        .requestMatchers(HttpMethod.GET, "/api/analytics/network/**").hasAnyRole("HOSPITAL_ADMIN", "PLATFORM_ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
