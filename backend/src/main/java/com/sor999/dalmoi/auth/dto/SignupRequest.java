package com.sor999.dalmoi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record SignupRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password, @NotBlank @Size(max = 50) String nickname) {
    public SignupRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        nickname = nickname == null ? null : nickname.strip();
    }
}
