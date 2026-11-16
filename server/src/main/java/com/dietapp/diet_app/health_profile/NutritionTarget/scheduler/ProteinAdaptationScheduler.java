package com.dietapp.diet_app.health_profile.NutritionTarget.scheduler;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ScheduledEvaluationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ScheduledEvaluationOutcome;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinAdaptationService;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinClinicalReviewResolver;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.ProteinCalculator;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Daily trigger for the protein-adaptation ramp-up.
 *
 * <p>This class only <em>selects</em> profiles and isolates failures. All
 * adaptation rules (day counting, the 7-day gate, the 10 g step, the clamp to
 * the long-term target, tolerance holds, the once-per-day marker) live in
 * {@link ProteinAdaptationService#evaluateScheduled} and are not repeated here.</p>
 *
 * <h3>Calendar day</h3>
 * <p>"Today" is {@code LocalDate.now(zone)} where {@code zone} is the
 * explicitly configured {@code app.timezone}; the cron trigger uses the same
 * zone. The JVM default zone is never consulted.</p>
 *
 * <h3>Query count</h3>
 * <p>Per batch of {@code batch-size} profiles: one query for the profiles and
 * their latest reported intake, and one query for the clinical-review set.
 * The eligible stages ({@code STARTING}, {@code ADAPTING}) and the
 * {@code lastEvaluatedOn} marker are filtered in SQL, so skipped profiles are
 * never loaded. Each processed profile then costs the merge's version read and
 * its own write.</p>
 *
 * <h3>Transactions and concurrency</h3>
 * <p>This class is deliberately <strong>not</strong> transactional. Every
 * {@code evaluateScheduled} call runs in its own transaction (default
 * {@code READ_COMMITTED}), so a failure rolls back only that profile. Profiles
 * are detached, so the save is a merge that compares {@code @Version}: when
 * another instance or thread has already updated the row the merge fails with
 * an optimistic-locking exception, which is counted as a conflict
 * ("already handled") and never retried within the same run.</p>
 *
 * <h3>Logging</h3>
 * <p>Only counts, the processed date and profile row ids are logged. No intake,
 * target, tolerance or medical information is written to the log.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.scheduler.protein-adaptation.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ProteinAdaptationScheduler {

    /** Stages the scheduled process counts days for. */
    static final Set<ProteinAdaptationStage> ELIGIBLE_STAGES = Set.of(
            ProteinAdaptationStage.STARTING,
            ProteinAdaptationStage.ADAPTING);

    private static final UUID MIN_UUID = new UUID(0L, 0L);

    private final ProteinAdaptationProfileRepository repository;
    private final ProteinAdaptationService adaptationService;
    private final ProteinClinicalReviewResolver clinicalReviewResolver;
    private final Clock clock;
    private final int batchSize;

    @Autowired
    public ProteinAdaptationScheduler(
            ProteinAdaptationProfileRepository repository,
            ProteinAdaptationService adaptationService,
            ProteinClinicalReviewResolver clinicalReviewResolver,
            @Value("${app.timezone}") String timezone,
            @Value("${app.scheduler.protein-adaptation.batch-size:200}") int batchSize) {

        this(repository, adaptationService, clinicalReviewResolver,
                Clock.system(ZoneId.of(timezone)), batchSize);
    }

    ProteinAdaptationScheduler(
            ProteinAdaptationProfileRepository repository,
            ProteinAdaptationService adaptationService,
            ProteinClinicalReviewResolver clinicalReviewResolver,
            Clock clock,
            int batchSize) {

        if (batchSize < 1) {
            throw new IllegalArgumentException("batch-size must be at least 1");
        }
        this.repository = repository;
        this.adaptationService = adaptationService;
        this.clinicalReviewResolver = clinicalReviewResolver;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    /** Scheduled entry point: once a day, in the configured application timezone. */
    @Scheduled(
            cron = "${app.scheduler.protein-adaptation.cron:0 30 0 * * *}",
            zone = "${app.timezone}")
    public void runDaily() {
        try {
            run(LocalDate.now(clock));
        } catch (RuntimeException e) {
            // A failure in batch selection must not kill the scheduler thread.
            log.error("Protein adaptation run aborted ({})", e.getClass().getName());
        }
    }

    /**
     * Processes every eligible profile for {@code today}. Safe to call any
     * number of times for the same date: already-evaluated profiles are
     * excluded by the query and, defensively, by {@code evaluateScheduled}.
     */
    public ProteinAdaptationRunSummary run(LocalDate today) {

        Map<ScheduledEvaluationOutcome, Integer> outcomes =
                new EnumMap<>(ScheduledEvaluationOutcome.class);
        int examined = 0;
        int conflicts = 0;
        int failures = 0;

        UUID afterId = MIN_UUID;
        while (true) {
            List<EligibleProteinProfile> batch =
                    repository.findEligibleForScheduledEvaluation(
                            ELIGIBLE_STAGES, today, afterId,
                            PageRequest.of(0, batchSize));
            if (batch.isEmpty()) {
                break;
            }
            examined += batch.size();
            afterId = batch.get(batch.size() - 1).profile().getId();

            Set<UUID> needingReview = clinicalReviewResolver
                    .findRequiringClinicalReview(batch.stream()
                            .map(EligibleProteinProfile::healthProfileId)
                            .collect(Collectors.toSet()));

            for (EligibleProteinProfile row : batch) {
                Set<MedicalCondition> conditions =
                        needingReview.contains(row.healthProfileId())
                                ? ProteinCalculator.CLINICAL_REVIEW_CONDITIONS
                                : Set.of();
                try {
                    ScheduledEvaluationResult result =
                            adaptationService.evaluateScheduled(
                                    row.profile(), conditions, today,
                                    row.latestReportedIntakeGrams());
                    outcomes.merge(result.outcome(), 1, Integer::sum);

                } catch (OptimisticLockingFailureException e) {
                    conflicts++;
                    log.info("Protein adaptation profile {} already updated by "
                            + "another worker; skipped", row.profile().getId());

                } catch (RuntimeException e) {
                    failures++;
                    log.error("Protein adaptation profile {} failed ({})",
                            row.profile().getId(), e.getClass().getName());
                }
            }

            if (batch.size() < batchSize) {
                break;
            }
        }

        ProteinAdaptationRunSummary summary = new ProteinAdaptationRunSummary(
                today, examined, Map.copyOf(outcomes), conflicts, failures);
        log.info("Protein adaptation run for {} finished: examined={}, outcomes={}, "
                        + "conflicts={}, failures={}",
                today, examined, outcomes, conflicts, failures);
        return summary;
    }
}
