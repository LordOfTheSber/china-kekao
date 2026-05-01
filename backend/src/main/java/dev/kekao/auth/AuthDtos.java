package dev.kekao.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @Email @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 200) String password
    ) {}

    public record LoginRequest(
            @Email @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(min = 1, max = 200) String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenPair(String accessToken, String refreshToken) {}
}
