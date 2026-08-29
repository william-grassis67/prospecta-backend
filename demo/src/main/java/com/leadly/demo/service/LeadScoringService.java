package com.leadly.demo.service;

import com.leadly.demo.entity.Lead;
import org.springframework.stereotype.Service;

@Service
public class LeadScoringService {

    /**
     * Calcula a pontuação do Lead de 0 a 100 com base no nível de contato e presença web.
     */
    public Integer calcularScore(Lead lead) {
        int score = 20; // Pontuação base por existência

        if (lead.getNumeroTelefone() != null && !lead.getNumeroTelefone().isBlank()) {
            score += 25;
        }
        if (lead.getEmail() != null && !lead.getEmail().isBlank()) {
            score += 20;
        }
        if (lead.getWebsite() != null && !lead.getWebsite().isBlank()) {
            score += 20;
        }
        if (lead.getInstagram() != null && !lead.getInstagram().isBlank()) {
            score += 15;
        }

        return Math.min(score, 100);
    }
}