package com.leadly.demo.dto;

/**
 * Resultado de uma busca de leads via OpenStreetMap (Overpass/Nominatim).
 *
 * Separado de {@link LeadDTO} de propósito: o LeadDTO representa um lead já
 * salvo no CRM (com status, prioridade, próximo contato etc.), enquanto este
 * DTO representa um resultado bruto de busca geográfica, que ainda não foi
 * adicionado aos contatos do usuário. Os nomes dos campos seguem exatamente
 * o que o frontend (js/leads.js) já espera em cada lead retornado por
 * GET /api/leads/search: nome, tipo, endereco, telefone, email, website,
 * latitude, longitude.
 *
 * Quando o usuário clicar em "Adicionar aos contatos", estes dados devem
 * ser convertidos para um {@link CreateLeadRequest} (nomeEmpresa, categoria,
 * numeroTelefone, cidade/estado/pais etc.) antes de persistir no CRM.
 */
public record LeadSearchResultDTO(
        String nome,
        String telefone,
        String email,
        String website,
        String instagram,
        String endereco,
        Double latitude,
        Double longitude,
        String tipo
) {}