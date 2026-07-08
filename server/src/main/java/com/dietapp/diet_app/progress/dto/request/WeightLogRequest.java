package com.dietapp.diet_app.progress.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WeightLogRequest {
    private BigDecimal weightKg;
    private LocalDate logDate;
}
