package com.leadly.demo.service;

import com.leadly.demo.entity.MessageResponse;
import com.leadly.demo.entity.User;
import com.leadly.demo.repository.LeadRepository;
import com.leadly.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class MenssageAutoService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;

    public MenssageAutoService(
            LeadRepository leadRepository,
            UserRepository userRepository
    ) {
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
    }

    public MessageResponse messageResponse(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado")
                );

        String message = """
                Olá! Tudo bem?

                Meu nome é %s e trabalho com desenvolvimento
                de soluções digitais para empresas.

                Gostaria de apresentar uma solução que pode ajudar
                sua empresa a melhorar sua presença digital e
                conquistar mais clientes.

                Podemos conversar?
                """.formatted(user.getName());

        return new MessageResponse(
                message,
                true
        );
    }
}