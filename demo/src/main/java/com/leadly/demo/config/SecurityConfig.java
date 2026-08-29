package com.leadly.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =====================================================
                // CORS
                // =====================================================

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                // =====================================================
                // CSRF
                // =====================================================

                .csrf(csrf ->
                        csrf.disable()
                )

                // =====================================================
                // SESSÃO
                // =====================================================

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // =====================================================
                // SECURITY CONTEXT
                // =====================================================

                .securityContext(context ->
                        context.requireExplicitSave(false)
                )

                // =====================================================
                // AUTORIZAÇÃO
                // =====================================================

                .authorizeHttpRequests(auth -> auth

                        // ---------------------------------------------
                        // OPTIONS
                        // ---------------------------------------------

                        .requestMatchers(
                                org.springframework.http.HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // ---------------------------------------------
                        // AUTENTICAÇÃO
                        // ---------------------------------------------

                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/verify-reset-code",
                                "/api/auth/reset-password"
                        ).permitAll()

                        // ---------------------------------------------
                        // TESTE OSM
                        // ---------------------------------------------

                        .requestMatchers(
                                "/api/leads/test"
                        ).permitAll()

                        // ---------------------------------------------
                        // BUSCA DE LEADS
                        // ---------------------------------------------

                        .requestMatchers(
                                "/api/leads/search"
                        ).authenticated()

                        // ---------------------------------------------
                        // OUTRAS ROTAS DE LEADS
                        // ---------------------------------------------

                        .requestMatchers(
                                "/api/leads/**"
                        ).authenticated()

                        // ---------------------------------------------
                        // RESTANTE
                        // ---------------------------------------------

                        .anyRequest().authenticated()
                )

                // =====================================================
                // JWT
                // =====================================================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // =============================================================
    // PASSWORD ENCODER
    // =============================================================

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    // =============================================================
    // CORS
    // =============================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // =========================================================
        // ORIGENS PERMITIDAS
        // -----------------------------------------------------------
        // DEV: IntelliJ (63342), Live Server (5500), front local (3000)
        // PROD: quando o Leadly estiver no ar, adicione aqui o domínio
        //       real (ex.: "https://app.leadly.com.br") e, se quiser,
        //       remova as origens de localhost.
        // =========================================================

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:63342",
                        "http://127.0.0.1:63342",
                        "http://localhost:5500",
                        "http://127.0.0.1:5500",
                        "http://localhost:3000",
                        "http://127.0.0.1:3000"
                        // "https://app.leadly.com.br" // <-- produção, quando existir
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}