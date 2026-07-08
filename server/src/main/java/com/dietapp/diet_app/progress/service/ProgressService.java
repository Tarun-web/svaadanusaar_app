package com.dietapp.diet_app.progress.service;

import com.dietapp.diet_app.progress.dto.request.WeightLogRequest;
import com.dietapp.diet_app.progress.dto.response.WeightLogResponse;
import com.dietapp.diet_app.progress.entity.WeightLog;
import com.dietapp.diet_app.progress.repository.WeightLogRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProgressService {

    private final WeightLogRepository repo;


    /**
     * Log or update weight for a given day
     */
    public void logWeight(UUID userId, WeightLogRequest request) {

        LocalDate date =
                request.getLogDate() != null
                        ? request.getLogDate()
                        : LocalDate.now();

        // Check if a log already exists for the user and date, if not create a new one
        WeightLog log = repo
                .findByUserIdAndLogDate(userId, date)
                .orElse(
                        WeightLog.builder()
                                .id(UUID.randomUUID())
                                .userId(userId)
                                .logDate(date)
                                .createdAt(Instant.now())
                                .build()
                );

        log.setWeightKg(request.getWeightKg());

        repo.save(log);
    }

    /**
     * Latest logged weight
     */
    public WeightLogResponse latest(UUID userId) {
        return repo.findTopByUserIdOrderByLogDateDesc(userId)
                .map(w -> new WeightLogResponse(
                        w.getWeightKg(),
                        w.getLogDate()
                ))
                .orElse(null);
    }

    /**
     * Full history
     */
    public List<WeightLogResponse> history(UUID userId) {
        return repo.findByUserIdOrderByLogDateDesc(userId)
                .stream()
                .map(w -> new WeightLogResponse(
                        w.getWeightKg(),
                        w.getLogDate()
                ))
                .toList();
    }
}
