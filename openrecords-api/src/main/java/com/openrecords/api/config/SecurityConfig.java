package com.openrecords.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import com.openrecords.api.security.CurrentUserFilter;
import com.openrecords.api.security.JwtAuthenticationFilter;

/**
 * Initial security configuration.
 *
 * For now: permit all requests. This lets us verify infrastructure end-to-end
 * without dealing with authentication. We'll add JWT-based authentication
 * in a dedicated auth phase once the domain model is in place.
 * 
 * Phase 6: add JwtAuthenticationFilter to validate tokens and populate CurrentUser.
 */
@Configuration
public class SecurityConfig {

    /**
    * Initial security configuration.
    */
    // @Bean
    // public SecurityFilterChain securityFilterChain(
    //         HttpSecurity http,
    //         CurrentUserFilter currentUserFilter) throws Exception {
    //     http
    //             .csrf(csrf -> csrf.disable())
    //             .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
    //             // Run our filter early — it just identifies the user from header.
    //             .addFilterBefore(currentUserFilter, UsernamePasswordAuthenticationFilter.class);

    //     return http.build();
    // }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            // JWT validation; CurrentUser populated for downstream service-layer checks
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}