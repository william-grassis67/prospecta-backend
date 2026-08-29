package com.leadly.demo.controller;

import com.leadly.demo.dto.ForgotPasswordRequest;
import com.leadly.demo.dto.LoginRequest;
import com.leadly.demo.dto.ResetPasswordRequest;
import com.leadly.demo.dto.VerifyResetCodeRequest;
import com.leadly.demo.entity.User;
import com.leadly.demo.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/api/auth")
@RestController
public class authController {

    private final AuthService authService;

    public authController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        return ResponseEntity.ok(authService.registerUser(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                authService.login(
                        request.getEmail(),
                        request.getPassword()
                )
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request.getEmail());

        return ResponseEntity.ok(
                Map.of("message", "Se o e-mail estiver cadastrado, enviaremos um código de recuperação.")
        );
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(
            @RequestBody VerifyResetCodeRequest request) {

        String resetToken = authService.verifyResetCode(
                request.getEmail(),
                request.getCode()
        );

        return ResponseEntity.ok(
                Map.of("resetToken", resetToken)
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(
                request.getToken(),
                request.getPassword()
        );

        return ResponseEntity.ok(
                Map.of("message", "Senha alterada com sucesso")
        );
    }

    // Trata os erros lançados pelo AuthService (email não encontrado, código
    // errado/expirado, senha inválida etc.) como 400 com mensagem amigável,
    // em vez de deixar virar 500 com stack trace.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleAuthError(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}