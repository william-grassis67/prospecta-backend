package com.leadly.demo.config;

import com.leadly.demo.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        System.out.println();
        System.out.println("========================================");
        System.out.println("JWT FILTER");
        System.out.println("Método: " + request.getMethod());
        System.out.println("Path: " + path);

        // =====================================================
        // OPTIONS
        // =====================================================

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            System.out.println("JWT: OPTIONS");

            filterChain.doFilter(request, response);
            return;
        }

        // =====================================================
        // AUTHORIZATION
        // =====================================================

        String authorization =
                request.getHeader("Authorization");

        if (authorization == null ||
                authorization.isBlank()) {

            System.out.println(
                    "JWT: Authorization ausente"
            );

            filterChain.doFilter(request, response);
            return;
        }

        System.out.println(
                "JWT: Authorization recebido"
        );

        // =====================================================
        // BEARER
        // =====================================================

        if (!authorization.startsWith("Bearer ")) {

            System.out.println(
                    "JWT: Authorization não começa com Bearer"
            );

            filterChain.doFilter(request, response);
            return;
        }

        // =====================================================
        // TOKEN
        // =====================================================

        String token =
                authorization
                        .substring(7)
                        .trim();

        if (token.isBlank()) {

            System.out.println(
                    "JWT: token vazio"
            );

            filterChain.doFilter(request, response);
            return;
        }

        System.out.println(
                "JWT: token recebido"
        );

        System.out.println(
                "JWT: tamanho = " + token.length()
        );

        // =====================================================
        // VALIDAR
        // =====================================================

        try {

            boolean valido =
                    jwtService.tokenValido(token);

            System.out.println(
                    "JWT: válido = " + valido
            );

            if (!valido) {

                System.out.println(
                        "JWT: token inválido/expirado"
                );

                SecurityContextHolder.clearContext();

                filterChain.doFilter(request, response);
                return;
            }

            // =================================================
            // EMAIL
            // =================================================

            String email =
                    jwtService.extrairEmail(token);

            System.out.println(
                    "JWT: email = " + email
            );

            if (email == null ||
                    email.isBlank()) {

                System.out.println(
                        "JWT: email não encontrado"
                );

                SecurityContextHolder.clearContext();

                filterChain.doFilter(request, response);
                return;
            }

            // =================================================
            // AUTENTICAÇÃO
            // =================================================

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.emptyList()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            // =================================================
            // SECURITY CONTEXT
            // =================================================

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            System.out.println(
                    "JWT: USUÁRIO AUTENTICADO"
            );

            System.out.println(
                    "JWT: authenticated = " +
                            authentication.isAuthenticated()
            );

            System.out.println(
                    "JWT: principal = " +
                            authentication.getPrincipal()
            );

            System.out.println(
                    "JWT: authorities = " +
                            authentication.getAuthorities()
            );

        } catch (Exception e) {

            System.out.println(
                    "JWT: ERRO"
            );

            System.out.println(
                    "Tipo: " +
                            e.getClass().getSimpleName()
            );

            System.out.println(
                    "Mensagem: " +
                            e.getMessage()
            );

            SecurityContextHolder.clearContext();
        }

        // =====================================================
        // CONTINUAR
        // =====================================================

        filterChain.doFilter(request, response);

        // =====================================================
        // RESULTADO
        // =====================================================

        System.out.println(
                "JWT: status final = " +
                        response.getStatus()
        );

        System.out.println(
                "========================================"
        );
    }
}