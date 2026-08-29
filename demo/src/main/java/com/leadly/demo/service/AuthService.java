package com.leadly.demo.service;

import com.leadly.demo.dto.UserDTO;
import com.leadly.demo.entity.PasswordResetToken;
import com.leadly.demo.entity.User;
import com.leadly.demo.enums.TipoUsuario;
import com.leadly.demo.repository.PasswordResetTokenRepository;
import com.leadly.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;


@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_EXPIRATION_MINUTES = 10;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService, PasswordResetTokenRepository passwordResetTokenRepository){
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    //REGISTER A NEW USER
    public User registerUser(User user){
        String passwordEncrypted = passwordEncoder.encode(user.getPassword());
        UserDTO newUserDTO = new UserDTO();
        if (user.getName().trim().isEmpty() && user.getEmail().trim().isEmpty() && user.getPassword().trim().isEmpty()){
            throw new RuntimeException("Fields cannot be empty");
        }
        user.setTipoUsuario(TipoUsuario.USUARIO);
        newUserDTO.setEmail(user.getEmail());
        newUserDTO.setName(user.getName());
        user.setPassword(passwordEncrypted);

        emailService.enviarEmail(user.getEmail(), "Bem-vindo ao Leadly", "Olá, \n" + user.getName() +
                "\n" +
                "Seja muito bem-vindo ao **Leadly**! \uD83D\uDE80\n" +
                "\n" +
                "Sua conta foi criada com sucesso e você já pode começar a utilizar a plataforma para encontrar novas oportunidades e potencializar sua prospecção.\n" +
                "\n" +
                "A partir de agora, o Leadly está com você para tornar sua busca por clientes mais simples, rápida e eficiente.\n" +
                "\n" +
                "Bons negócios! \uD83D\uDC99\n" +
                "\n" +
                "**Equipe Leadly**\n");

        User savedUser = userRepository.save(user);
        return savedUser;
    }

    public String login(String email, String password){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("This email not found"));

        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new RuntimeException("Email or password invalid");
        }

        return jwtService.gerarToken(user.getEmail());
    }

    public void forgotPassword(String email) {

        Optional<User> userOpt = userRepository.findByEmail(email);

        // Não revela se o e-mail está cadastrado ou não.
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        String code = gerarCodigoSeisDigitos();

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByUser(user)
                .orElseGet(PasswordResetToken::new);

        resetToken.setUser(user);
        resetToken.setCode(code);
        resetToken.setExpiration(LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES));
        resetToken.setUsed(false);

        passwordResetTokenRepository.save(resetToken);

        emailService.enviarEmail(
                user.getEmail(),
                "Código de recuperação de senha - Leadly",
                "Olá, " + user.getName() + "!\n\n"
                        + "Recebemos uma solicitação para redefinir sua senha.\n\n"
                        + "Seu código de verificação é:\n\n"
                        + code
                        + "\n\n"
                        + "Esse código expira em " + CODE_EXPIRATION_MINUTES + " minutos.\n\n"
                        + "Se você não solicitou essa recuperação, ignore este e-mail.\n\n"
                        + "Equipe Leadly"
        );
    }

    /**
     * Valida o código de 6 dígitos e retorna um token temporário (JWT)
     * que autoriza a troca de senha em resetPassword().
     */
    public String verifyResetCode(String email, String code) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Código inválido ou expirado"));

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByUser(user)
                .orElseThrow(() -> new RuntimeException("Código inválido ou expirado"));

        if (resetToken.isUsed()) {
            throw new RuntimeException("Código inválido ou expirado");
        }

        if (resetToken.getExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Código expirado. Solicite um novo código.");
        }

        if (!resetToken.getCode().equals(code)) {
            throw new RuntimeException("O código informado está incorreto.");
        }

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return jwtService.gerarResetToken(user.getEmail());
    }

    /**
     * Troca a senha usando o token temporário emitido por verifyResetCode().
     * Não aceita mais o código de 6 dígitos nem UUID diretamente.
     */
    public void resetPassword(String resetToken, String newPassword) {

        String email = jwtService.validarResetTokenExtrairEmail(resetToken);

        if (email == null) {
            throw new RuntimeException("Não foi possível alterar sua senha. Solicite um novo código.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Não foi possível alterar sua senha."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String gerarCodigoSeisDigitos() {
        int numero = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", numero);
    }
}