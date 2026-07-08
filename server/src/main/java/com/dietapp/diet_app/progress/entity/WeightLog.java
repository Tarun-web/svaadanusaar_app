package com.dietapp.diet_app.progress.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "weight_logs",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "log_date"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightLog {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Date for which weight is logged (one per day)
     */
    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "weight_kg", precision = 5, scale = 2, nullable = false)
    private BigDecimal weightKg;



    @Column(name = "created_at")
    private Instant createdAt;
}
