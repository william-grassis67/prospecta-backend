package com.leadly.demo.dto;

import com.leadly.demo.entity.PrioridadeLead;
import com.leadly.demo.entity.StatusLead;

import java.time.LocalDate;

public record LeadDTO(
        String nomeEmpresa,
        String numeroTelefone,
        String categoria,
        String email,
        String instagram,
        String pais,
        String cidade,
        String estado,
        String website,
        StatusLead status,
        PrioridadeLead prioridade,
        String observacao,
        LocalDate proximoContato
) {}