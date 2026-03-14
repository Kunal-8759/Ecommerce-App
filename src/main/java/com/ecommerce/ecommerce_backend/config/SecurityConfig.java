package com.ecommerce.ecommerce_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
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

import com.ecommerce.ecommerce_backend.service.CustomUserDetailsService;
import com.ecommerce.ecommerce_backend.service.UserService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity 
@RequiredArgsConstructor
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userService;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    private final JwtAuthenticationEntryPoint unauthorizedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
            
        return authProvider;
    }

    @Bean 
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            .cors(Customizer.withDefaults()) 
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler) 
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/users/register",
                        "/api/users/login",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                .requestMatchers("/api/cart/**").hasRole("CUSTOMER")
                .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers("/api/payments/**").hasRole("CUSTOMER")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) 
            .authenticationProvider(authenticationProvider()) 
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
