package com.dietapp.diet_app.progress.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class WeightLogResponse {
    private BigDecimal weightKg;
    private LocalDate logDate;
}
