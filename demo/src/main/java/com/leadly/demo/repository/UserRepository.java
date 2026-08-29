package com.leadly.demo.repository;

import com.leadly.demo.entity.User;
import com.leadly.demo.enums.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

 Optional<User> findByEmail(String email);

 // Se houver busca por tipo de usuário, deve receber TipoUsuario
 List<User> findByTipoUsuario(TipoUsuario tipoUsuario);
}
