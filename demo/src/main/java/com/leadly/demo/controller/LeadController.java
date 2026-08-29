package com.leadly.demo.controller;

import com.leadly.demo.dto.CreateLeadRequest;
import com.leadly.demo.dto.LeadResponseDTO;
import com.leadly.demo.dto.LeadSearchResultDTO;
import com.leadly.demo.dto.UpdateLeadRequest;
import com.leadly.demo.service.LeadService;
import com.leadly.demo.service.OpenStreetMapService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final OpenStreetMapService openStreetMapService;
    private final LeadService leadService;

    public LeadController(
            OpenStreetMapService openStreetMapService,
            LeadService leadService
    ) {
        this.openStreetMapService = openStreetMapService;
        this.leadService = leadService;
    }

    // --- BUSCA EXTERNA (MANTIDA) ---

    @GetMapping("/search")
    public ResponseEntity<List<LeadSearchResultDTO>> searchLeads(
            @RequestParam String tipo,
            @RequestParam String pais,
            @RequestParam(required = false) String estado,
            @RequestParam String localizacao
    ) {
        List<LeadSearchResultDTO> leads = openStreetMapService.buscarLeads(
                tipo,
                pais,
                estado,
                localizacao
        );
        return ResponseEntity.ok(leads);
    }

    // --- GERENCIAMENTO DE LEADS SALVOS DO USUÁRIO ---

    @PostMapping
    public ResponseEntity<LeadResponseDTO> salvarLead(
            @RequestBody CreateLeadRequest request,
            Authentication authentication
    ) {
        LeadResponseDTO salvo = leadService.salvarLead(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public ResponseEntity<List<LeadResponseDTO>> getMyLeads(
            Authentication authentication
    ) {
        List<LeadResponseDTO> leads = leadService.listarMeusLeads(authentication);
        return ResponseEntity.ok(leads);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponseDTO> buscarLeadPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        LeadResponseDTO lead = leadService.buscarLeadPorId(id, authentication);
        return ResponseEntity.ok(lead);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeadResponseDTO> atualizarLead(
            @PathVariable Long id,
            @RequestBody UpdateLeadRequest request,
            Authentication authentication
    ) {
        LeadResponseDTO atualizado = leadService.atualizarLead(id, request, authentication);
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirLead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        leadService.excluirLead(id, authentication);
        return ResponseEntity.noContent().build();
    }
}