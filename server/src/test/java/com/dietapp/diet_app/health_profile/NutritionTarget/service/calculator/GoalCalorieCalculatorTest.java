package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CalorieTargetResult;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.Occupation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit tests (no Spring, no database) for {@link GoalCalorieCalculator}.
 *
 * <p>Reference person: 30-year-old male, 80 kg, 180 cm, OFFICE, no exercise.
 * His TDEE is 2252 kcal (see {@code EnergyCalculatorsTest}). 7700 kcal = 1 kg.</p>
 */
class GoalCalorieCalculatorTest {

    private static final BigDecimal TDEE = new BigDecimal("2252");

    private final GoalCalorieCalculator calc = new GoalCalorieCalculator(
            new TdeeCalculator(new BmrCalculator(), new ActivityLevelResolver(),
                    new ActivityFactorStrategy(), new NeatCalculator(), new TefCalculator()));

    private static PersonalProfile person() {
        return PersonalProfile.builder()
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusYears(30).minusDays(1))
                .heightCm(180.0)
                .weightKg(new BigDecimal("80"))
                .occupation(Occupation.OFFICE)
                .build();
    }

    private static FitnessGoalProfile goal(Goal g, Double targetKg, LocalDate date, Double weekly) {
        return FitnessGoalProfile.builder()
                .primaryGoal(g).targetWeightKg(targetKg).targetDate(date)
                .weeklyWeightChangeKg(weekly).build();
    }

    private CalorieTargetResult run(FitnessGoalProfile g) {
        return calc.calculate(person(), g, null);
    }

    // ── goals that equal TDEE / fixed adjustments ──────────────────────

    @Test
    void maintenance_equalsTdee() {
        CalorieTargetResult r = run(goal(Goal.MAINTENANCE, null, null, null));

        assertThat(r.getTdee()).isEqualByComparingTo(TDEE);
        assertThat(r.getProposedCalories()).isEqualByComparingTo(TDEE);
        assertThat(r.getDailyCalorieAdjustment()).isEqualByComparingTo("0");
        assertThat(r.isTimelineAchievable()).isTrue();
    }

    @Test
    void generalHealth_andBodyRecomposition_equalTdee() {
        assertThat(run(goal(Goal.GENERAL_HEALTH, null, null, null)).getProposedCalories())
                .isEqualByComparingTo(TDEE);
        assertThat(run(goal(Goal.BODY_RECOMPOSITION, null, null, null)).getProposedCalories())
                .isEqualByComparingTo(TDEE);
    }

    @Test
    void sportsPerformance_addsFixed200() {
        CalorieTargetResult r = run(goal(Goal.SPORTS_PERFORMANCE, null, null, null));

        assertThat(r.getDailyCalorieAdjustment()).isEqualByComparingTo("200");
        assertThat(r.getProposedCalories()).isEqualByComparingTo("2452");
    }

    // ── fat loss ───────────────────────────────────────────────────────

    @Test
    void fatLoss_defaultHalfKgPerWeek() {
        // 0.50 kg/week * 7700 / 7 = 550.00 kcal/day deficit
        CalorieTargetResult r = run(goal(Goal.FAT_LOSS, 70.0, LocalDate.now().plusWeeks(40), null));

        assertThat(r.getEffectiveWeeklyWeightChangeKg()).isEqualByComparingTo("0.50");
        assertThat(r.getRequestedWeeklyWeightChangeKg()).isNull();
        assertThat(r.getDailyCalorieAdjustment()).isEqualByComparingTo("-550.00");
        assertThat(r.getProposedCalories()).isEqualByComparingTo("1702");
        assertThat(r.isTimelineAchievable()).isTrue();
    }

    @Test
    void fatLoss_requestedRate_isUsed() {
        // 0.25 kg/week => 275.00 deficit
        CalorieTargetResult r = run(goal(Goal.FAT_LOSS, 75.0, LocalDate.now().plusWeeks(40), 0.25));

        assertThat(r.getDailyCalorieAdjustment()).isEqualByComparingTo("-275.00");
        assertThat(r.getProposedCalories()).isEqualByComparingTo("1977");
    }

    @Test
    void fatLoss_rateBelowMinimum_isRaisedToPointZeroFive() {
        // 0.05 * 7700 / 7 = 55.00
        CalorieTargetResult r = run(goal(Goal.FAT_LOSS, 79.0, LocalDate.now().plusWeeks(40), 0.01));

        assertThat(r.getRequestedWeeklyWeightChangeKg()).isEqualByComparingTo("0.01");
        assertThat(r.getEffectiveWeeklyWeightChangeKg()).isEqualByComparingTo("0.05");
        assertThat(r.getDailyCalorieAdjustment()).isEqualByComparingTo("-55.00");
    }

    @Test
    void fatLoss_impossibleTimeline_isFlagged_butStillCalculated() {
        // 10 kg in 2 weeks at 0.5 kg/week
        CalorieTargetResult r = run(goal(Goal.FAT_LOSS, 70.0, LocalDate.now().plusWeeks(2), null));

        assertThat(r.isTimelineAchievable()).isFalse();
        assertThat(r.getProposedCalories()).isEqualByComparingTo("1702");
    }

    @Test
    void fatLoss_timelineExactlyAchievable() {
        // 1 kg at 0.5 kg/week needs 2 weeks = 14 days
        CalorieTargetResult r = run(goal(Goal.FAT_LOSS, 79.0, LocalDate.now().plusDays(14), null));

        assertThat(r.isTimelineAchievable()).isTrue();
    }

    @Test
    void fatLoss_requiresTargetWeightBelowCurrent() {
        assertThatThrownBy(() -> run(goal(Goal.FAT_LOSS, null, LocalDate.now().plusWeeks(8), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> run(goal(Goal.FAT_LOSS, 80.0, LocalDate.now().plusWeeks(8), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> run(goal(Goal.FAT_LOSS, 85.0, LocalDate.now().plusWeeks(8), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fatLoss_requiresFutureTargetDate() {
        assertThatThrownBy(() -> run(goal(Goal.FAT_LOSS, 70.0, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        // today is "not in the future" for weight goals
        assertThatThrownBy(() -> run(goal(Goal.FAT_LOSS, 70.0, LocalDate.now(), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── weight gain ────────────────────────────────────────────────────

    @Test
    void weightGain_addsSurplus_withoutTheMuscleGainCap() {
        // 0.75 kg/week => 825.00 kcal/day; weight gain is NOT capped at 400
        CalorieTargetResult r = run(goal(Goal.WEIGHT_GAIN, 85.0, LocalDate.now().plusWeeks(20), 0.75));

        assertThat(r.getDailyCalorieAdjustment()).isEqualByComparingTo("825.00");
        assertThat(r.getProposedCalories()).isEqualByComparingTo("3077");
    }

    @Test
    void weightGain_requiresTargetWeightAboveCurrent_andFutureDate() {
        assertThatThrownBy(() -> run(goal(Goal.WEIGHT_GAIN, 80.0, LocalDate.now().plusWeeks(8), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> run(goal(Goal.WEIGHT_GAIN, null, LocalDate.now().plusWeeks(8), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> run(goal(Goal.WEIGHT_GAIN, 85.0, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── muscle gain ────────────────────────────────────────────────────

    @Test
    void muscleGain_surplusIsCappedAt400() {
        // default 0.50 kg/week => 550 -> capped to 400
        CalorieTargetResult r = run(goal(Goal.MUSCLE_GAIN, null, null, null));

        assertThat(r.getEffectiveWeeklyWeightChangeKg()).isEqualByComparingTo("0.50");
        assertThat(r.getDailyCalorieAdjustment()).isEqualByComparingTo("400");
        assertThat(r.getProposedCalories()).isEqualByComparingTo("2652");
    }

    @Test
    void muscleGain_smallRequestedRate_isNotRaised() {
        // 0.2 kg/week => 220.00 (under the cap)
        CalorieTargetResult r = run(goal(Goal.MUSCLE_GAIN, null, null, 0.2));

        assertThat(r.getDailyCalorieAdjustment()).isEqualByComparingTo("220.00");
        assertThat(r.getProposedCalories()).isEqualByComparingTo("2472");
    }

    @Test
    void muscleGain_doesNotRequireTargetWeightOrDate() {
        CalorieTargetResult r = run(goal(Goal.MUSCLE_GAIN, null, null, null));

        assertThat(r.getTargetWeightKg()).isNull();
        assertThat(r.getTargetDate()).isNull();
        assertThat(r.isTimelineAchievable()).isTrue();
    }

    // ── shared validation ──────────────────────────────────────────────

    @Test
    void pastTargetDate_isRejectedForEveryGoal() {
        for (Goal g : Goal.values()) {
            assertThatThrownBy(() -> run(goal(g, 70.0, LocalDate.now().minusDays(1), null)))
                    .as(g.name())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("past");
        }
    }

    @Test
    void nonPositiveWeeklyChange_isRejected() {
        assertThatThrownBy(() -> run(goal(Goal.MUSCLE_GAIN, null, null, 0.0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> run(goal(Goal.MUSCLE_GAIN, null, null, -0.5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingInputs_areRejected() {
        FitnessGoalProfile noGoal = goal(null, null, null, null);
        PersonalProfile noWeight = person();
        noWeight.setWeightKg(null);
        PersonalProfile zeroWeight = person();
        zeroWeight.setWeightKg(BigDecimal.ZERO);

        assertThatThrownBy(() -> calc.calculate(null, goal(Goal.MAINTENANCE, null, null, null), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.calculate(person(), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.calculate(person(), noGoal, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.calculate(noWeight, goal(Goal.MAINTENANCE, null, null, null), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.calculate(zeroWeight, goal(Goal.MAINTENANCE, null, null, null), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resultEchoesGoalAndInputs() {
        LocalDate date = LocalDate.now().plusWeeks(30);
        CalorieTargetResult r = run(goal(Goal.FAT_LOSS, 72.5, date, null));

        assertThat(r.getGoal()).isEqualTo(Goal.FAT_LOSS);
        assertThat(r.getCurrentWeightKg()).isEqualByComparingTo("80");
        assertThat(r.getTargetWeightKg()).isEqualByComparingTo("72.5");
        assertThat(r.getTargetDate()).isEqualTo(date);
    }

    @Test
    void proposedCalories_isWholeNumber() {
        assertThat(run(goal(Goal.FAT_LOSS, 75.0, LocalDate.now().plusWeeks(40), 0.33))
                .getProposedCalories().scale()).isZero();
    }
}
