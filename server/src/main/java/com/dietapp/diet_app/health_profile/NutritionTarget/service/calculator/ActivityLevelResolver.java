package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ActivityLevel;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.Occupation;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Resolves the user's overall lifestyle/activity classification.
 *
 * IMPORTANT:
 * This class does NOT calculate TDEE or calories.
 *
 * It considers:
 *
 * 1. Occupation
 * 2. Structured exercise
 * 3. Exercise frequency
 * 4. Exercise duration
 * 5. Exercise intensity
 * 6. Activity type
 * 7. Multiple activities
 * 8. Night-shift status
 *
 * The resulting ActivityLevel is later used by the
 * nutrition-target calculation engine.
 */
@Component
public class ActivityLevelResolver {

    /*
     * Maximum contribution from occupational activity.
     *
     * This prevents occupation from completely dominating
     * structured exercise.
     */
    private static final double MAX_OCCUPATION_SCORE = 0.60;

    /*
     * Maximum contribution from structured exercise.
     */
    private static final double MAX_EXERCISE_SCORE = 1.00;

    /*
     * Small adjustment for night-shift work.
     *
     * Night shift does NOT automatically mean inactive.
     */
    private static final double NIGHT_SHIFT_PENALTY = 0.05;


    /**
     * Resolve overall activity level.
     *
     * @param personalProfile user's personal profile
     * @param activities user's structured workout activities
     * @return resolved ActivityLevel
     */
    public ActivityLevel resolve(
            PersonalProfile personalProfile,
            Collection<ActivityProfile> activities
    ) {

        if (personalProfile == null) {
            return ActivityLevel.SEDENTARY;
        }

        /*
         * ---------------------------------------------------------
         * 1. OCCUPATIONAL ACTIVITY
         * ---------------------------------------------------------
         */
        double occupationScore =
                resolveOccupationScore(
                        personalProfile.getOccupation()
                );

        /*
         * ---------------------------------------------------------
         * 2. STRUCTURED EXERCISE
         * ---------------------------------------------------------
         */
        double exerciseScore =
                resolveExerciseScore(
                        activities
                );

        /*
         * ---------------------------------------------------------
         * 3. COMBINE OCCUPATION + EXERCISE
         * ---------------------------------------------------------
         *
         * Do not simply add both scores without limits.
         *
         * Otherwise:
         *
         * MANUAL_LABOUR
         * +
         * HIGH intensity gym
         *
         * could become artificially extreme.
         */
        double score =
                combineScores(
                        occupationScore,
                        exerciseScore
                );

        /*
         * ---------------------------------------------------------
         * 4. NIGHT SHIFT
         * ---------------------------------------------------------
         *
         * Night shift does not mean sedentary.
         *
         * We therefore apply only a very small adjustment.
         */
        if (Boolean.TRUE.equals(
                personalProfile.getNightShift()
        )) {

            score -= NIGHT_SHIFT_PENALTY;
        }

        /*
         * Never allow a negative score.
         */
        score = Math.max(0.0, score);

        /*
         * ---------------------------------------------------------
         * 5. MAP SCORE → ACTIVITY LEVEL
         * ---------------------------------------------------------
         */
        return mapScoreToActivityLevel(score);
    }


    /**
     * Combines occupational and structured exercise activity.
     *
     * We use diminishing returns for occupation so that
     * extremely physically demanding occupations do not
     * completely dominate the classification.
     */
    private double combineScores(
            double occupationScore,
            double exerciseScore
    ) {

        occupationScore =
                Math.clamp(occupationScore, 0.0,
                        MAX_OCCUPATION_SCORE
                );

        exerciseScore =
                Math.clamp(exerciseScore, 0.0,
                        MAX_EXERCISE_SCORE
                );

        /*
         * Occupation contributes strongly when the person
         * is physically active during the day.
         *
         * Exercise receives a slightly larger weight because
         * structured training is more measurable.
         */
        return
                (occupationScore * 0.75)
                        +
                        (exerciseScore);
    }


    // ============================================================
    // OCCUPATION
    // ============================================================

    /**
     * Resolves baseline activity contribution from occupation.
     */
    private double resolveOccupationScore(
            Occupation occupation
    ) {

        if (occupation == null) {
            /*
             * Unknown occupation should not automatically
             * classify the user as sedentary.
             */
            return 0.10;
        }

        return switch (occupation) {

            /*
             * Predominantly sitting.
             */
            case OFFICE ->
                    0.10;

            /*
             * Usually sedentary, but can vary.
             */
            case WFH ->
                    0.08;

            /*
             * Students generally have mixed movement.
             */
            case HOSTEL_STUDENT,
                 COLLEGE_STUDENT ->
                    0.15;

            /*
             * Household work can involve considerable
             * standing and movement.
             */
            case HOME_MAKER ->
                    0.30;

            /*
             * Activity varies significantly.
             */
            case RETIRED ->
                    0.10;

            /*
             * Shift work varies depending on the actual job.
             */
            case SHIFT_WORKER ->
                    0.20;

            /*
             * Usually substantial occupational movement.
             */
            case MANUAL_LABOUR ->
                    0.60;
        };
    }


    // ============================================================
    // STRUCTURED EXERCISE
    // ============================================================

    /**
     * Calculates the contribution of structured exercise.
     *
     * Multiple activities are supported.
     */
    private double resolveExerciseScore(
            Collection<ActivityProfile> activities
    ) {

        if (activities == null || activities.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;

        for (ActivityProfile activity : activities) {

            if (!isValidActivity(activity)) {
                continue;
            }

            /*
             * -----------------------------------------------------
             * Weekly training volume
             * -----------------------------------------------------
             */
            double weeklyHours =
                    activity.getDaysPerWeek()
                            * activity.getDurationMinutes()
                            / 60.0;

            /*
             * -----------------------------------------------------
             * Volume
             * -----------------------------------------------------
             */
            double volumeScore =
                    resolveVolumeScore(
                            weeklyHours
                    );

            /*
             * -----------------------------------------------------
             * Intensity
             * -----------------------------------------------------
             */
            double intensityScore =
                    resolveIntensityScore(
                            activity.getIntensity()
                    );

            /*
             * -----------------------------------------------------
             * Activity type
             * -----------------------------------------------------
             */
            double activityDemand =
                    resolveActivityDemand(
                            activity.getActivityType()
                    );

            /*
             * -----------------------------------------------------
             * Activity contribution
             * -----------------------------------------------------
             */
            double activityScore =
                    volumeScore
                            * intensityScore
                            * activityDemand;

            totalScore += activityScore;
        }

        /*
         * Prevent multiple activities from creating
         * an unrealistic classification.
         */
        return Math.min(
                totalScore,
                MAX_EXERCISE_SCORE
        );
    }


    /**
     * Basic validation for an ActivityProfile.
     */
    private boolean isValidActivity(
            ActivityProfile activity
    ) {

        if (activity == null) {
            return false;
        }

        if (activity.getDaysPerWeek() == null) {
            return false;
        }

        if (activity.getDurationMinutes() == null) {
            return false;
        }

        if (activity.getDaysPerWeek() <= 0) {
            return false;
        }

        return activity.getDurationMinutes() > 0;
    }


    // ============================================================
    // EXERCISE VOLUME
    // ============================================================

    /**
     * Converts weekly exercise hours into a normalized score.
     *
     * This is a classification score, NOT calories.
     */
    private double resolveVolumeScore(
            double weeklyHours
    ) {

        if (weeklyHours <= 0) {
            return 0.0;
        }

        /*
         * < 1 hour/week
         */
        if (weeklyHours < 1.0) {
            return 0.05;
        }

        /*
         * 1–2 hours/week
         */
        if (weeklyHours < 2.0) {
            return 0.10;
        }

        /*
         * 2–3 hours/week
         */
        if (weeklyHours < 3.0) {
            return 0.20;
        }

        /*
         * 3–5 hours/week
         */
        if (weeklyHours < 5.0) {
            return 0.30;
        }

        /*
         * 5–7 hours/week
         */
        if (weeklyHours < 7.0) {
            return 0.40;
        }

        /*
         * 7–10 hours/week
         */
        if (weeklyHours < 10.0) {
            return 0.50;
        }

        /*
         * 10+ hours/week
         */
        return 0.60;
    }


    // ============================================================
    // INTENSITY
    // ============================================================

    /**
     * Converts workout intensity into a classification factor.
     */
    private double resolveIntensityScore(
            WorkoutIntensity intensity
    ) {

        if (intensity == null) {
            return 1.0;
        }

        return switch (intensity) {

            case LOW ->
                    0.75;

            case MODERATE ->
                    1.00;

            case HIGH ->
                    1.25;

            /*
             * ATHLETE currently exists in your enum.
             *
             * We treat it as very high training demand,
             * but cap the final exercise score.
             */
            case ATHLETE ->
                    1.40;
        };
    }


    // ============================================================
    // ACTIVITY TYPE
    // ============================================================

    /**
     * Estimates relative activity demand.
     *
     * This is NOT an energy expenditure multiplier.
     */
    private double resolveActivityDemand(
            ActivityType activityType
    ) {

        if (activityType == null) {
            return 1.0;
        }

        return switch (activityType) {

            /*
             * Not structured exercise.
             */
            case SEDENTARY ->
                    0.0;

            /*
             * Lower-intensity movement.
             */
            case WALKING ->
                    0.80;

            /*
             * Primarily mobility / mind-body activities.
             */
            case YOGA,
                 PILATES ->
                    0.70;

            /*
             * Resistance training.
             */
            case GYM,
                 HOME_WORKOUT ->
                    1.00;

            /*
             * Generally higher cardiovascular demand.
             */
            case CYCLING,
                 RUNNING,
                 SWIMMING ->
                    1.15;

            /*
             * Variable but potentially high demand.
             */
            case SPORTS ->
                    1.20;

            /*
             * Typically high-intensity activity.
             */
            case MARTIAL_ARTS ->
                    1.25;

            /*
             * Compatibility with your current enum.
             *
             * Ideally remove PHYSICALLY_DEMANDING_JOB from
             * ActivityType because occupational activity is
             * already represented by Occupation.
             */
            case PHYSICALLY_DEMANDING_JOB ->
                    1.00;
        };
    }


    // ============================================================
    // FINAL CLASSIFICATION
    // ============================================================

    /**
     * Maps normalized activity score to ActivityLevel.
     */
    private ActivityLevel mapScoreToActivityLevel(
            double score
    ) {

        /*
         * 0.00 – 0.099
         */
        if (score < 0.10) {
            return ActivityLevel.SEDENTARY;
        }

        /*
         * 0.10 – 0.349
         */
        if (score < 0.35) {
            return ActivityLevel.LIGHTLY_ACTIVE;
        }

        /*
         * 0.35 – 0.699
         */
        if (score < 0.70) {
            return ActivityLevel.MODERATELY_ACTIVE;
        }

        /*
         * 0.70 – 1.099
         */
        if (score < 1.10) {
            return ActivityLevel.VERY_ACTIVE;
        }

        /*
         * 1.10+
         */
        return ActivityLevel.EXTREMELY_ACTIVE;
    }
}