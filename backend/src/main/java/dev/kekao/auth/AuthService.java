package dev.kekao.auth;

import dev.kekao.auth.AuthDtos.LoginRequest;
import dev.kekao.auth.AuthDtos.RefreshRequest;
import dev.kekao.auth.AuthDtos.RegisterRequest;
import dev.kekao.auth.AuthDtos.TokenPair;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import dev.kekao.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final AuthProperties props;

    @Autowired
    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       TokenService tokens,
                       AuthProperties props) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.props = props;
    }

    @Transactional
    public TokenPair register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(UserRole.ROLE_USER)
                .settings(new HashMap<>())
                .build();
        users.save(user);
        return issuePair(user);
    }

    @Transactional
    public TokenPair login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        UserEntity user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issuePair(user);
    }

    @Transactional
    public TokenPair refresh(RefreshRequest req) {
        String hash = tokens.hashRefreshToken(req.refreshToken());
        RefreshTokenEntity stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }
        stored.setRevoked(true);
        refreshTokens.save(stored);
        return issuePair(stored.getUser());
    }

    private TokenPair issuePair(UserEntity user) {
        String access = tokens.issueAccessToken(user);
        String refresh = tokens.generateRefreshToken();
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .tokenHash(tokens.hashRefreshToken(refresh))
                .user(user)
                .expiresAt(Instant.now().plus(props.refreshTokenTtl()))
                .revoked(false)
                .build();
        refreshTokens.save(entity);
        return new TokenPair(access, refresh);
    }

    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException() { super("Email already in use"); }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() { super("Invalid credentials"); }
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException() { super("Invalid refresh token"); }
    }
}
