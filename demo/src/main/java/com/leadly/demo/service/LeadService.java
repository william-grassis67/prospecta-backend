package com.leadly.demo.service;

import com.leadly.demo.dto.CreateLeadRequest;
import com.leadly.demo.dto.LeadResponseDTO;
import com.leadly.demo.dto.UpdateLeadRequest;
import com.leadly.demo.entity.Lead;
import com.leadly.demo.entity.PrioridadeLead;
import com.leadly.demo.entity.StatusLead;
import com.leadly.demo.entity.User;
import com.leadly.demo.repository.LeadRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadScoringService leadScoringService;
    private final UserService userService;

    public LeadService(LeadRepository leadRepository,
                       LeadScoringService leadScoringService,
                       UserService userService) {
        this.leadRepository = leadRepository;
        this.leadScoringService = leadScoringService;
        this.userService = userService;
    }

    @Transactional
    public LeadResponseDTO salvarLead(CreateLeadRequest request, Authentication authentication) {
        User user = userService.getMe(authentication);
        verificarDuplicidade(request.website(), request.numeroTelefone(), request.nomeEmpresa(), request.cidade(), user);

        Lead lead = new Lead();
        lead.setNomeEmpresa(request.nomeEmpresa());
        lead.setNumeroTelefone(request.numeroTelefone());
        lead.setCategoria(request.categoria());
        lead.setEmail(request.email());
        lead.setInstagram(request.instagram());
        lead.setPais(request.pais());
        lead.setCidade(request.cidade());
        lead.setEstado(request.estado());
        lead.setWebsite(request.website());
        lead.setObservacao(request.observacao());
        lead.setProximoContato(request.proximoContato());

        // Status e Prioridade padrão
        lead.setStatus(request.status() != null ? request.status() : StatusLead.NOVO);
        lead.setPrioridade(request.prioridade() != null ? request.prioridade() : PrioridadeLead.MEDIA);

        // Associa obrigatoriamente ao usuário logado
        lead.setUser(user);

        // Define Lead Score (preserva valor fornecido ou calcula via serviço)
        if (request.leadScore() != null) {
            lead.setLeadScore(request.leadScore());
        } else {
            lead.setLeadScore(leadScoringService.calcularScore(lead));
        }

        Lead salvo = leadRepository.save(lead);
        return LeadResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<LeadResponseDTO> listarMeusLeads(Authentication authentication) {
        User user = userService.getMe(authentication);
        return leadRepository.findByUser(user).stream()
                .map(LeadResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeadResponseDTO buscarLeadPorId(Long id, Authentication authentication) {
        User user = userService.getMe(authentication);
        Lead lead = leadRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado ou acesso negado."));
        return LeadResponseDTO.fromEntity(lead);
    }

    @Transactional
    public LeadResponseDTO atualizarLead(Long id, UpdateLeadRequest request, Authentication authentication) {
        User user = userService.getMe(authentication);
        Lead lead = leadRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado ou acesso negado."));

        if (request.nomeEmpresa() != null) lead.setNomeEmpresa(request.nomeEmpresa());
        if (request.numeroTelefone() != null) lead.setNumeroTelefone(request.numeroTelefone());
        if (request.categoria() != null) lead.setCategoria(request.categoria());
        if (request.email() != null) lead.setEmail(request.email());
        if (request.instagram() != null) lead.setInstagram(request.instagram());
        if (request.pais() != null) lead.setPais(request.pais());
        if (request.cidade() != null) lead.setCidade(request.cidade());
        if (request.estado() != null) lead.setEstado(request.estado());
        if (request.website() != null) lead.setWebsite(request.website());
        if (request.observacao() != null) lead.setObservacao(request.observacao());
        if (request.proximoContato() != null) lead.setProximoContato(request.proximoContato());
        if (request.status() != null) lead.setStatus(request.status());
        if (request.prioridade() != null) lead.setPrioridade(request.prioridade());

        lead.setLeadScore(leadScoringService.calcularScore(lead));

        Lead atualizado = leadRepository.save(lead);
        return LeadResponseDTO.fromEntity(atualizado);
    }

    @Transactional
    public void excluirLead(Long id, Authentication authentication) {
        User user = userService.getMe(authentication);
        Lead lead = leadRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado ou acesso negado."));
        leadRepository.delete(lead);
    }

    private void verificarDuplicidade(String website, String telefone, String nomeEmpresa, String cidade, User user) {
        if (website != null && !website.isBlank() && leadRepository.existsByUserAndWebsite(user, website)) {
            throw new IllegalArgumentException("Já existe um Lead com este website na sua conta.");
        }
        if (telefone != null && !telefone.isBlank() && leadRepository.existsByUserAndNumeroTelefone(user, telefone)) {
            throw new IllegalArgumentException("Já existe um Lead com este número de telefone na sua conta.");
        }
        if (nomeEmpresa != null && cidade != null && leadRepository.existsByUserAndNomeEmpresaAndCidade(user, nomeEmpresa, cidade)) {
            throw new IllegalArgumentException("Já existe um Lead registrado com este nome e cidade na sua conta.");
        }
    }
}