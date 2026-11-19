package com.sor999.dalmoi.gps.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.time.Instant;
import java.util.UUID;

public record GpsRequest(@NotNull UUID eventId, @NotNull @Min(0) Long sequence,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotNull @DecimalMin("0") Double accuracyMeters, @NotNull Instant recordedAt) {
}
