package com.vitacare.api.config;

import com.vitacare.api.security.TokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    public SecurityConfig(TokenAuthenticationFilter tokenAuthenticationFilter) {
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
          .csrf(csrf -> csrf.disable())
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth
              .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/docs/**",
                "/webjars/**"
              ).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/nutritionists").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/nutritionists/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/plans/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/nutritionists/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/nutritionists/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/nutritionists/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/plans/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/plans/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/plans/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/subscriptions/**").authenticated()
                .requestMatchers("/api/clients/**").authenticated()
                .anyRequest().authenticated()
          )
          .httpBasic(httpBasic -> httpBasic.disable())
          .formLogin(form -> form.disable())
          .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}