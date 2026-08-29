package com.leadly.demo.repository;

import com.leadly.demo.entity.Lead;
import com.leadly.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    // Retorna todos os Leads do usuário logado
    List<Lead> findByUser(User user);

    // Busca um Lead garantindo isolamento de propriedade (multi-tenancy)
    Optional<Lead> findByIdAndUser(Long id, User user);

    // Métodos para verificação de duplicidade por usuário
    boolean existsByUserAndWebsite(User user, String website);
    boolean existsByUserAndNumeroTelefone(User user, String numeroTelefone);
    boolean existsByUserAndNomeEmpresaAndCidade(User user, String nomeEmpresa, String cidade);
}