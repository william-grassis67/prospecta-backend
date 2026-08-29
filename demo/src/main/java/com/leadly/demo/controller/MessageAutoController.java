package com.leadly.demo.controller;

import com.leadly.demo.entity.MessageResponse;
import com.leadly.demo.service.MenssageAutoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageAutoController {

    private final MenssageAutoService menssageAutoService;

    public MessageAutoController(MenssageAutoService menssageAutoService) {
        this.menssageAutoService = menssageAutoService;
    }

    @PostMapping("/generate")
    public ResponseEntity<MessageResponse> generateMessage(
            Authentication authentication
    ) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                menssageAutoService.messageResponse(email)
        );
    }
}