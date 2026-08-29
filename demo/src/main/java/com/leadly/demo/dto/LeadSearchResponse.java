package com.leadly.demo.dto;

public class LeadSearchResponse {

    private String id;
    private String nome;
    private String endereco;
    private String telefone;
    private String site;
    private Double avaliacao;
    private String googleMapsUrl;
    private String instagram;

    public String getId(){
        return this.id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getNome(){
        return this.nome;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public String getEndereco(){
        return this.endereco;
    }

    public void setEndereco(String endereco){
        this.endereco = endereco;
    }

    public String getTelefone(){
        return this.telefone;
    }

    public void setTelefone(String telefone){
        this.telefone = telefone;
    }

    public String getSite(){
        return this.site;
    }

    public void setSite(String site){
        this.site = site;
    }

    public Double getAvaliacao(){
        return this.avaliacao;
    }

    public void setAvaliacao(Double avaliacao){
        this.avaliacao = avaliacao;
    }

    public String getGoogleMapsUrl(){
        return this.googleMapsUrl;
    }

    public void setGoogleMapsUrl(String googleMapsUrl){
        this.googleMapsUrl = googleMapsUrl;
    }

    public String getInstagram(){
        return this.instagram;
    }

    public void setInstagram(String instagram){
        this.instagram = instagram;
    }
}