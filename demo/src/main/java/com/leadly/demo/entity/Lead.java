package com.leadly.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "tb_lead")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeEmpresa;
    private String numeroTelefone;
    private String categoria;
    private String email;
    private String instagram;

    private String pais;
    private String cidade;
    private String estado;

    private String website;

    private Integer leadScore;

    @Enumerated(EnumType.STRING)
    private StatusLead status;

    @Enumerated(EnumType.STRING)
    private PrioridadeLead prioridade;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(nullable = false, updatable = false)
    private LocalDate dataAdicionado;

    private LocalDate proximoContato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public Lead() {
    }

    public Lead(Long id, String nomeEmpresa, String numeroTelefone, String categoria,
                String email, String instagram, String pais, String cidade, String estado) {
        this.id = id;
        this.nomeEmpresa = nomeEmpresa;
        this.numeroTelefone = numeroTelefone;
        this.categoria = categoria;
        this.email = email;
        this.instagram = instagram;
        this.pais = pais;
        this.cidade = cidade;
        this.estado = estado;
    }

    @PrePersist
    protected void onCreate() {
        if (this.dataAdicionado == null) {
            this.dataAdicionado = LocalDate.now();
        }
        if (this.status == null) {
            this.status = StatusLead.NOVO;
        }
        if (this.prioridade == null) {
            this.prioridade = PrioridadeLead.MEDIA;
        }
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public String getNumeroTelefone() {
        return numeroTelefone;
    }

    public void setNumeroTelefone(String numeroTelefone) {
        this.numeroTelefone = numeroTelefone;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    // CORREÇÃO: Método getCidade() corrigido para retornar this.cidade
    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Integer getLeadScore() {
        return leadScore;
    }

    public void setLeadScore(Integer leadScore) {
        this.leadScore = leadScore;
    }

    public StatusLead getStatus() {
        return status;
    }

    public void setStatus(StatusLead status) {
        this.status = status;
    }

    public PrioridadeLead getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(PrioridadeLead prioridade) {
        this.prioridade = prioridade;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDate getDataAdicionado() {
        return dataAdicionado;
    }

    public void setDataAdicionado(LocalDate dataAdicionado) {
        this.dataAdicionado = dataAdicionado;
    }

    public LocalDate getProximoContato() {
        return proximoContato;
    }

    public void setProximoContato(LocalDate proximoContato) {
        this.proximoContato = proximoContato;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}