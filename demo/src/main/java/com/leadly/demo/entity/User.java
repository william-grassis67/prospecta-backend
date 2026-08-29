package com.leadly.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leadly.demo.entity.Lead;
import com.leadly.demo.enums.TipoUsuario;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    /*
     * A coluna tipo_usuario no PostgreSQL é SMALLINT.
     *
     * EnumType.ORDINAL faz a conversão:
     *
     * ADMINISTRADOR = 0
     * USUARIO       = 1
     *
     * Isso é compatível com os valores existentes no banco.
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "tipo_usuario", nullable = false)
    private TipoUsuario tipoUsuario = TipoUsuario.USUARIO;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<Lead> leads = new ArrayList<>();

    public User() {
    }

    public User(
            Long id,
            String name,
            String email,
            String password,
            TipoUsuario tipoUsuario
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.tipoUsuario = tipoUsuario != null
                ? tipoUsuario
                : TipoUsuario.USUARIO;
    }

    // Métodos auxiliares para manter a consistência bidirecional

    public void addLead(Lead lead) {
        leads.add(lead);
        lead.setUser(this);
    }

    public void removeLead(Lead lead) {
        leads.remove(lead);
        lead.setUser(null);
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public List<Lead> getLeads() {
        return leads;
    }

    public void setLeads(List<Lead> leads) {
        this.leads = leads;
    }
}

