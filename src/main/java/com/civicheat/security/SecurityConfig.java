package com.civicheat.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration enforcing stateless JWT authentication, role-based access control, and CORS policies.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Public Documentation and OpenAPI
                        .requestMatchers(
                                "/",
                                "/*.html",
                                "/*.css",
                                "/*.js",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/favicon.ico",
                                "/actuator/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Public Auth Endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()

                        // Public Heatmap & Incident Visualization
                        .requestMatchers(HttpMethod.GET, "/api/v1/heatmap/**").permitAll()

                        // Public Uploaded Media Assets
                        .requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()

                        // Public Department Listings
                        .requestMatchers(HttpMethod.GET, "/api/v1/departments/**").permitAll()

                        // Public Complaint Search & Detail (Notice /mine is restricted)
                        .requestMatchers(HttpMethod.GET, "/api/v1/complaints/mine").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/complaints", "/api/v1/complaints/{id}").permitAll()

                        // Citizen / Authenticated User Operations
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/complaints").hasAnyRole("CITIZEN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/complaints/{id}/upvote").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")

                        // Officer & Admin Status Updates
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/complaints/{id}/status").hasAnyRole("OFFICER", "ADMIN")

                        // Admin Only Operations
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/complaints/{id}/reassign").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/departments/**").hasRole("ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
