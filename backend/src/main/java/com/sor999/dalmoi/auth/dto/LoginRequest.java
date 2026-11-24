package com.sor999.dalmoi.auth.dto;

import com.sor999.dalmoi.global.validation.Utf8ByteLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Utf8ByteLength(max = 72) @Schema(description = "UTF-8 기준 최대 72바이트") String password) {
    public LoginRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
