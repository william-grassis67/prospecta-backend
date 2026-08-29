 package com.leadly.demo.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    /*
     * =========================================================
     * CONFIGURAÇÃO JWT
     * =========================================================
     */

    private static final String SECRET_KEY =
            "leadly-chave-secreta-super-segura-2026";

    private static final long EXPIRATION_TIME =
            24 * 60 * 60 * 1000L;

    /*
     * =========================================================
     * GERAR TOKEN
     * =========================================================
     */

    public String gerarToken(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email não pode ser vazio."
            );
        }

        Date agora = new Date();

        Date expiracao = new Date(
                agora.getTime() + EXPIRATION_TIME
        );

        return Jwts.builder()
                .subject(email)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(getKey())
                .compact();
    }

    /*
     * =========================================================
     * EXTRAIR EMAIL
     * =========================================================
     */

    public String extrairEmail(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Token não pode ser vazio."
            );
        }

        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /*
     * =========================================================
     * VALIDAR TOKEN
     * =========================================================
     */

    public boolean tokenValido(String token) {

        if (token == null || token.isBlank()) {
            System.out.println("JWT: TOKEN VAZIO");
            return false;
        }

        try {

            Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);

            System.out.println("JWT: TOKEN VALIDO");

            return true;

        } catch (Exception e) {

            System.out.println(
                    "JWT: TOKEN INVALIDO"
            );

            System.out.println(
                    "JWT ERRO: "
                            + e.getClass().getSimpleName()
            );

            System.out.println(
                    "JWT MENSAGEM: "
                            + e.getMessage()
            );

            return false;
        }
    }

    /*
     * =========================================================
     * EXTRAIR CLAIMS
     * =========================================================
     */

    public Claims extrairClaims(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Token não pode ser vazio."
            );
        }

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /*
     * =========================================================
     * CHAVE JWT
     * =========================================================
     */

    private SecretKey getKey() {

        return Keys.hmacShaKeyFor(
                SECRET_KEY.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    private static final long RESET_TOKEN_EXPIRATION_TIME =
            5 * 60 * 1000L; // 5 minutos

    /*
     * =========================================================
     * GERAR TOKEN TEMPORÁRIO DE RESET DE SENHA
     * =========================================================
     */

    public String gerarResetToken(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email não pode ser vazio."
            );
        }

        Date agora = new Date();

        Date expiracao = new Date(
                agora.getTime() + RESET_TOKEN_EXPIRATION_TIME
        );

        return Jwts.builder()
                .subject(email)
                .claim("purpose", "password_reset")
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(getKey())
                .compact();
    }

    /*
     * =========================================================
     * VALIDAR TOKEN TEMPORÁRIO DE RESET E EXTRAIR EMAIL
     * =========================================================
     */

    public String validarResetTokenExtrairEmail(String token) {

        if (token == null || token.isBlank()) {
            return null;
        }

        try {

            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String purpose = claims.get("purpose", String.class);

            if (!"password_reset".equals(purpose)) {
                return null;
            }

            return claims.getSubject();

        } catch (Exception e) {
            return null;
        }
    }
}
