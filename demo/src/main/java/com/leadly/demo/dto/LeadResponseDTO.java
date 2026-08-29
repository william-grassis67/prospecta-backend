package com.leadly.demo.dto;

import com.leadly.demo.entity.Lead;
import com.leadly.demo.entity.PrioridadeLead;
import com.leadly.demo.entity.StatusLead;

import java.time.LocalDate;

public record LeadResponseDTO(
        Long id,
        String nomeEmpresa,
        String numeroTelefone,
        String categoria,
        String email,
        String instagram,
        String pais,
        String cidade,
        String estado,
        String website,
        Integer leadScore,
        StatusLead status,
        PrioridadeLead prioridade,
        String observacao,
        LocalDate dataAdicionado,
        LocalDate proximoContato
) {
    public static LeadResponseDTO fromEntity(Lead lead) {
        return new LeadResponseDTO(
                lead.getId(),
                lead.getNomeEmpresa(),
                lead.getNumeroTelefone(),
                lead.getCategoria(),
                lead.getEmail(),
                lead.getInstagram(),
                lead.getPais(),
                lead.getCidade(),
                lead.getEstado(),
                lead.getWebsite(),
                lead.getLeadScore(),
                lead.getStatus(),
                lead.getPrioridade(),
                lead.getObservacao(),
                lead.getDataAdicionado(),
                lead.getProximoContato()
        );
    }
}