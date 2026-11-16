package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.BmrResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.TdeeResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ActivityLevel;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.Occupation;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit tests (no Spring, no database) for the energy-side calculators
 * that previously had no dedicated tests: BMR, NEAT, TEF, fibre, activity
 * factor, activity-level resolution and TDEE.
 *
 * <p>Every expected number is derived by hand from the formula in the
 * production class, shown in the comment next to it.</p>
 */
class EnergyCalculatorsTest {

    // ── fixtures ───────────────────────────────────────────────────────

    private static PersonalProfile person(
            Gender gender, int age, double heightCm, String weightKg, Occupation occupation) {
        return PersonalProfile.builder()
                .gender(gender)
                .dateOfBirth(LocalDate.now().minusYears(age))
                .heightCm(heightCm)
                .weightKg(new BigDecimal(weightKg))
                .occupation(occupation)
                .build();
    }

    /** 30-year-old male, 80 kg, 180 cm: BMR = 800 + 1125 - 150 + 5 = 1780.00 */
    private static PersonalProfile maleOffice() {
        return person(Gender.MALE, 30, 180, "80", Occupation.OFFICE);
    }

    private static ActivityProfile activity(
            ActivityType type, WorkoutIntensity intensity, Integer days, Integer minutes) {
        return ActivityProfile.builder()
                .activityType(type)
                .intensity(intensity)
                .daysPerWeek(days)
                .durationMinutes(minutes)
                .build();
    }

    private static TdeeCalculator tdeeCalculator() {
        return new TdeeCalculator(new BmrCalculator(), new ActivityLevelResolver(),
                new ActivityFactorStrategy(), new NeatCalculator(), new TefCalculator());
    }

    // ══════════════════════════════════════════════════════════════════
    // BMR (Mifflin-St Jeor)
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class Bmr {

        private final BmrCalculator calc = new BmrCalculator();

        @Test
        void male_usesPlusFive() {
            BmrResult r = calc.calculate(maleOffice());

            assertThat(r.age()).isEqualTo(30);
            assertThat(r.bmr()).isEqualByComparingTo("1780.00");
        }

        @Test
        void female_usesMinus161() {
            // 10*60 + 6.25*165 - 5*25 - 161 = 600 + 1031.25 - 125 - 161 = 1345.25
            BmrResult r = calc.calculate(person(Gender.FEMALE, 25, 165, "60", Occupation.OFFICE));

            assertThat(r.bmr()).isEqualByComparingTo("1345.25");
        }

        @Test
        void fractionalWeight_isUsedExactly() {
            // 725 + 1093.75 - 200 + 5 = 1623.75
            BmrResult r = calc.calculate(person(Gender.MALE, 40, 175, "72.5", Occupation.OFFICE));

            assertThat(r.bmr()).isEqualByComparingTo("1623.75");
        }

        @Test
        void bornToday_hasAgeZero() {
            PersonalProfile p = person(Gender.MALE, 0, 170, "70", Occupation.OFFICE);

            BmrResult r = calc.calculate(p);

            // 700 + 1062.5 - 0 + 5
            assertThat(r.age()).isZero();
            assertThat(r.bmr()).isEqualByComparingTo("1767.50");
        }

        @Test
        void resultIsRoundedToTwoDecimals() {
            assertThat(calc.calculate(maleOffice()).bmr().scale()).isEqualTo(2);
        }

        @Test
        void birthdayTomorrow_stillPreviousAge() {
            PersonalProfile p = person(Gender.MALE, 30, 180, "80", Occupation.OFFICE);
            p.setDateOfBirth(LocalDate.now().minusYears(30).plusDays(1));

            assertThat(calc.calculate(p).age()).isEqualTo(29);
        }

        @Test
        void nullProfile_isRejected() {
            assertThatThrownBy(() -> calc.calculate(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void futureDateOfBirth_isRejected() {
            PersonalProfile p = maleOffice();
            p.setDateOfBirth(LocalDate.now().plusDays(1));

            assertThatThrownBy(() -> calc.calculate(p))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("future");
        }

        @Test
        void eachMissingRequiredField_isRejected() {
            PersonalProfile noDob = maleOffice();
            noDob.setDateOfBirth(null);
            PersonalProfile noWeight = maleOffice();
            noWeight.setWeightKg(null);
            PersonalProfile noHeight = maleOffice();
            noHeight.setHeightCm(null);
            PersonalProfile noGender = maleOffice();
            noGender.setGender(null);

            for (PersonalProfile p : List.of(noDob, noWeight, noHeight, noGender)) {
                assertThatThrownBy(() -> calc.calculate(p))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // NEAT
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class Neat {

        private final NeatCalculator calc = new NeatCalculator();

        @ParameterizedTest
        @CsvSource({
                "OFFICE,150.00", "WFH,120.00", "HOSTEL_STUDENT,200.00", "COLLEGE_STUDENT,200.00",
                "HOME_MAKER,300.00", "RETIRED,150.00", "SHIFT_WORKER,200.00", "MANUAL_LABOUR,450.00"
        })
        void percentageOfBmr_perOccupation(Occupation occupation, String expected) {
            assertThat(calc.calculate(new BigDecimal("1000"), occupation))
                    .isEqualByComparingTo(expected);
        }

        @Test
        void unknownOccupation_defaultsToFifteenPercent() {
            assertThat(calc.calculate(new BigDecimal("1000"), null)).isEqualByComparingTo("150.00");
        }

        @Test
        void nonPositiveOrMissingBmr_givesZero() {
            assertThat(calc.calculate(null, Occupation.OFFICE)).isEqualByComparingTo("0");
            assertThat(calc.calculate(BigDecimal.ZERO, Occupation.OFFICE)).isEqualByComparingTo("0");
            assertThat(calc.calculate(new BigDecimal("-5"), Occupation.OFFICE)).isEqualByComparingTo("0");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // TEF
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class Tef {

        private final TefCalculator calc = new TefCalculator();

        @Test
        void isTenPercent() {
            assertThat(calc.calculate(new BigDecimal("2000"))).isEqualByComparingTo("200.00");
        }

        @Test
        void roundsHalfUpToTwoDecimals() {
            // 1234.56 * 0.10 = 123.456 -> 123.46
            assertThat(calc.calculate(new BigDecimal("1234.56"))).isEqualByComparingTo("123.46");
        }

        @Test
        void nonPositiveOrMissingEnergy_givesZero() {
            assertThat(calc.calculate(null)).isEqualByComparingTo("0");
            assertThat(calc.calculate(BigDecimal.ZERO)).isEqualByComparingTo("0");
            assertThat(calc.calculate(new BigDecimal("-1"))).isEqualByComparingTo("0");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Fibre
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class Fibre {

        private final FibreCalculator calc = new FibreCalculator();

        @Test
        void is14gPer1000Kcal() {
            assertThat(calc.calculate(new BigDecimal("2000"))).isEqualByComparingTo("28.00");
            assertThat(calc.calculate(new BigDecimal("1850"))).isEqualByComparingTo("25.90");
        }

        @Test
        void roundsHalfUp() {
            // 1500.50 * 14 / 1000 = 21.007 -> 21.01
            assertThat(calc.calculate(new BigDecimal("1500.50"))).isEqualByComparingTo("21.01");
        }

        @Test
        void smallestPositiveInput_isAccepted() {
            assertThat(calc.calculate(new BigDecimal("1"))).isEqualByComparingTo("0.01");
        }

        @Test
        void nullZeroAndNegativeCalories_areRejected() {
            for (BigDecimal bad : new BigDecimal[]{null, BigDecimal.ZERO, new BigDecimal("-100")}) {
                assertThatThrownBy(() -> calc.calculate(bad))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Activity factor
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class ActivityFactor {

        private final ActivityFactorStrategy strategy = new ActivityFactorStrategy();

        @ParameterizedTest
        @CsvSource({
                "SEDENTARY,1.20", "LIGHTLY_ACTIVE,1.375", "MODERATELY_ACTIVE,1.55",
                "VERY_ACTIVE,1.725", "EXTREMELY_ACTIVE,1.90"
        })
        void factorPerLevel(ActivityLevel level, String expected) {
            assertThat(strategy.getFactor(level)).isEqualByComparingTo(expected);
        }

        @Test
        void unknownLevel_defaultsToSedentary() {
            assertThat(strategy.getFactor(null)).isEqualByComparingTo("1.20");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Activity level resolution
    //   score = clamp(occupation, 0, 0.60) * 0.75 + min(exercise, 1.0)
    //           - 0.05 if night shift, floored at 0
    //   <0.10 SEDENTARY | <0.35 LIGHTLY | <0.70 MODERATELY | <1.10 VERY | else EXTREMELY
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class LevelResolution {

        private final ActivityLevelResolver resolver = new ActivityLevelResolver();

        private ActivityLevel resolve(PersonalProfile p, ActivityProfile... activities) {
            return resolver.resolve(p, List.of(activities));
        }

        @Test
        void nullProfile_isSedentary() {
            assertThat(resolver.resolve(null, null)).isEqualTo(ActivityLevel.SEDENTARY);
        }

        @ParameterizedTest
        @CsvSource({
                // occupation score * 0.75 with no exercise
                "OFFICE,SEDENTARY",           // 0.075
                "WFH,SEDENTARY",              // 0.060
                "RETIRED,SEDENTARY",          // 0.075
                "HOSTEL_STUDENT,LIGHTLY_ACTIVE",   // 0.1125
                "COLLEGE_STUDENT,LIGHTLY_ACTIVE",  // 0.1125
                "SHIFT_WORKER,LIGHTLY_ACTIVE",     // 0.150
                "HOME_MAKER,LIGHTLY_ACTIVE",       // 0.225
                "MANUAL_LABOUR,MODERATELY_ACTIVE"  // 0.450
        })
        void occupationAlone(Occupation occupation, ActivityLevel expected) {
            PersonalProfile p = person(Gender.MALE, 30, 180, "80", occupation);

            assertThat(resolver.resolve(p, null)).isEqualTo(expected);
            assertThat(resolver.resolve(p, List.of())).isEqualTo(expected);
        }

        @Test
        void unknownOccupation_scoresLikeOffice() {
            PersonalProfile p = person(Gender.MALE, 30, 180, "80", null);

            assertThat(resolver.resolve(p, null)).isEqualTo(ActivityLevel.SEDENTARY);
        }

        @Test
        void exerciseLiftsTheLevel() {
            // 4 h/week gym, moderate: 0.30 * 1.0 * 1.0 = 0.30; + 0.075 = 0.375
            assertThat(resolve(maleOffice(),
                    activity(ActivityType.GYM, WorkoutIntensity.MODERATE, 4, 60)))
                    .isEqualTo(ActivityLevel.MODERATELY_ACTIVE);
        }

        @Test
        void weeklyHourBands_followTheDocumentedBoundaries() {
            // OFFICE contributes 0.075. Gym/moderate => demand 1.0, intensity 1.0.
            // 3.0 h is NOT < 3.0 so it falls in the 0.30 band => 0.375 => MODERATELY
            assertThat(resolve(maleOffice(),
                    activity(ActivityType.GYM, WorkoutIntensity.MODERATE, 3, 60)))
                    .isEqualTo(ActivityLevel.MODERATELY_ACTIVE);
            // 2.0 h => 0.20 band => 0.275 => LIGHTLY
            assertThat(resolve(maleOffice(),
                    activity(ActivityType.GYM, WorkoutIntensity.MODERATE, 2, 60)))
                    .isEqualTo(ActivityLevel.LIGHTLY_ACTIVE);
            // 0.5 h => 0.05 band => 0.125 => LIGHTLY
            assertThat(resolve(maleOffice(),
                    activity(ActivityType.GYM, WorkoutIntensity.MODERATE, 1, 30)))
                    .isEqualTo(ActivityLevel.LIGHTLY_ACTIVE);
        }

        @Test
        void sedentaryActivityType_addsNothing() {
            assertThat(resolve(maleOffice(),
                    activity(ActivityType.SEDENTARY, WorkoutIntensity.ATHLETE, 7, 120)))
                    .isEqualTo(ActivityLevel.SEDENTARY);
        }

        @Test
        void singleHeavyActivity_canReachVeryActive() {
            // 14 h/week => 0.60 * 1.40 (athlete) * 1.15 (running) = 0.966; + 0.075 = 1.041
            assertThat(resolve(maleOffice(),
                    activity(ActivityType.RUNNING, WorkoutIntensity.ATHLETE, 7, 120)))
                    .isEqualTo(ActivityLevel.VERY_ACTIVE);
        }

        @Test
        void exerciseScoreIsCappedAtOne_soOnlyOccupationCanPushToExtreme() {
            ActivityProfile huge =
                    activity(ActivityType.RUNNING, WorkoutIntensity.ATHLETE, 7, 120);

            // capped 1.0 + 0.075 = 1.075 => VERY_ACTIVE (not extreme, however much training)
            assertThat(resolve(maleOffice(), huge, huge, huge)).isEqualTo(ActivityLevel.VERY_ACTIVE);
            // capped 1.0 + 0.45 = 1.45 => EXTREMELY_ACTIVE
            assertThat(resolve(person(Gender.MALE, 30, 180, "80", Occupation.MANUAL_LABOUR),
                    huge, huge, huge)).isEqualTo(ActivityLevel.EXTREMELY_ACTIVE);
        }

        @Test
        void invalidActivitiesAreIgnored() {
            List<ActivityProfile> invalid = new java.util.ArrayList<>();
            invalid.add(null);
            invalid.add(activity(ActivityType.GYM, WorkoutIntensity.HIGH, null, 60));
            invalid.add(activity(ActivityType.GYM, WorkoutIntensity.HIGH, 4, null));
            invalid.add(activity(ActivityType.GYM, WorkoutIntensity.HIGH, 0, 60));
            invalid.add(activity(ActivityType.GYM, WorkoutIntensity.HIGH, 4, 0));
            invalid.add(activity(ActivityType.GYM, WorkoutIntensity.HIGH, -1, 60));

            assertThat(resolver.resolve(maleOffice(), invalid)).isEqualTo(ActivityLevel.SEDENTARY);
        }

        @Test
        void missingTypeOrIntensity_usesNeutralDefaults() {
            // 4 h/week, type and intensity unknown => 0.30 * 1.0 * 1.0
            assertThat(resolve(maleOffice(), activity(null, null, 4, 60)))
                    .isEqualTo(ActivityLevel.MODERATELY_ACTIVE);
        }

        @Test
        void nightShift_subtractsAndCanDropALevel() {
            PersonalProfile day = person(Gender.MALE, 30, 180, "80", Occupation.HOSTEL_STUDENT);
            PersonalProfile night = person(Gender.MALE, 30, 180, "80", Occupation.HOSTEL_STUDENT);
            night.setNightShift(true);

            // 0.1125 => LIGHTLY ; 0.1125 - 0.05 = 0.0625 => SEDENTARY
            assertThat(resolver.resolve(day, null)).isEqualTo(ActivityLevel.LIGHTLY_ACTIVE);
            assertThat(resolver.resolve(night, null)).isEqualTo(ActivityLevel.SEDENTARY);
        }

        @Test
        void nightShift_neverMakesScoreNegative() {
            PersonalProfile night = person(Gender.MALE, 30, 180, "80", Occupation.WFH);
            night.setNightShift(true);

            // 0.06 - 0.05 = 0.01 (>= 0): still sedentary, no exception
            assertThat(resolver.resolve(night, null)).isEqualTo(ActivityLevel.SEDENTARY);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // TDEE = (BMR + NEAT + exercise) + 10 % TEF, rounded to whole kcal.
    // The activity factor is reported but never multiplied in.
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class Tdee {

        private final TdeeCalculator calc = tdeeCalculator();

        @Test
        void noExercise_office() {
            // BMR 1780.00; NEAT 15% = 267.00; before TEF 2047.00; TEF 204.70; 2251.70 -> 2252
            TdeeResult r = calc.calculate(maleOffice(), null);

            assertThat(r.age()).isEqualTo(30);
            assertThat(r.bmr()).isEqualByComparingTo("1780.00");
            assertThat(r.neat()).isEqualByComparingTo("267.00");
            assertThat(r.exerciseCalories()).isEqualByComparingTo("0");
            assertThat(r.energyBeforeTef()).isEqualByComparingTo("2047.00");
            assertThat(r.tef()).isEqualByComparingTo("204.70");
            assertThat(r.tdee()).isEqualByComparingTo("2252");
            assertThat(r.tdee().scale()).isZero();
            assertThat(r.activityLevel()).isEqualTo(ActivityLevel.SEDENTARY);
            assertThat(r.activityFactor()).isEqualByComparingTo("1.20");
        }

        @Test
        void withGymSessions_addsExerciseCalories() {
            // MET 5.0; kcal/min = 5*3.5*80/200 = 7.0; session 420; weekly 1680; daily 240.00
            // before TEF 1780 + 267 + 240 = 2287.00; TEF 228.70; 2515.70 -> 2516
            TdeeResult r = calc.calculate(maleOffice(), List.of(
                    activity(ActivityType.GYM, WorkoutIntensity.MODERATE, 4, 60)));

            assertThat(r.exerciseCalories()).isEqualByComparingTo("240.00");
            assertThat(r.energyBeforeTef()).isEqualByComparingTo("2287.00");
            assertThat(r.tdee()).isEqualByComparingTo("2516");
            assertThat(r.activityLevel()).isEqualTo(ActivityLevel.MODERATELY_ACTIVE);
            assertThat(r.activityFactor()).isEqualByComparingTo("1.55");
        }

        @Test
        void activityFactorIsReportedButNotApplied() {
            TdeeResult r = calc.calculate(maleOffice(), List.of(
                    activity(ActivityType.GYM, WorkoutIntensity.MODERATE, 4, 60)));

            // If the 1.55 factor were applied TDEE would be about 2759 (1780 * 1.55).
            assertThat(r.tdee()).isNotEqualByComparingTo(
                    r.bmr().multiply(r.activityFactor()));
        }

        @Test
        void manualLabour_hasLargerNeat() {
            // NEAT 45% = 801.00; before TEF 2581.00; TEF 258.10; 2839.10 -> 2839
            TdeeResult r = calc.calculate(
                    person(Gender.MALE, 30, 180, "80", Occupation.MANUAL_LABOUR), null);

            assertThat(r.neat()).isEqualByComparingTo("801.00");
            assertThat(r.tdee()).isEqualByComparingTo("2839");
        }

        @Test
        void female_wfh() {
            // BMR 1345.25; NEAT 12% = 161.43; before TEF 1506.68; TEF 150.67 -> 1657.35 -> 1657
            TdeeResult r = calc.calculate(
                    person(Gender.FEMALE, 25, 165, "60", Occupation.WFH), null);

            assertThat(r.neat()).isEqualByComparingTo("161.43");
            assertThat(r.energyBeforeTef()).isEqualByComparingTo("1506.68");
            assertThat(r.tef()).isEqualByComparingTo("150.67");
            assertThat(r.tdee()).isEqualByComparingTo("1657");
        }

        @Test
        void exerciseWithSessionsThatCannotBeUsed_isIgnored() {
            List<ActivityProfile> unusable = new java.util.ArrayList<>();
            unusable.add(null);
            unusable.add(activity(null, WorkoutIntensity.HIGH, 4, 60));
            unusable.add(activity(ActivityType.GYM, null, 4, 60));
            unusable.add(activity(ActivityType.GYM, WorkoutIntensity.HIGH, 0, 60));
            unusable.add(activity(ActivityType.GYM, WorkoutIntensity.HIGH, 4, 0));
            unusable.add(activity(ActivityType.GYM, WorkoutIntensity.HIGH, null, 60));

            assertThat(calc.calculate(maleOffice(), unusable).exerciseCalories())
                    .isEqualByComparingTo("0");
        }

        @Test
        void walkingLowIntensity_usesTheMetTable() {
            // 70 kg: 2.5*3.5*70/200 = 3.0625 kcal/min; *30 min = 91.875; *7 days = 643.125;
            // /7 = 91.875 per day -> 91.88 (HALF_UP)
            TdeeResult r = calc.calculate(
                    person(Gender.MALE, 30, 180, "70", Occupation.OFFICE),
                    List.of(activity(ActivityType.WALKING, WorkoutIntensity.LOW, 7, 30)));

            assertThat(r.exerciseCalories()).isEqualByComparingTo("91.88");
        }

        @Test
        void higherIntensity_burnsMore() {
            BigDecimal low = calc.calculate(maleOffice(), List.of(
                    activity(ActivityType.RUNNING, WorkoutIntensity.LOW, 3, 45))).exerciseCalories();
            BigDecimal athlete = calc.calculate(maleOffice(), List.of(
                    activity(ActivityType.RUNNING, WorkoutIntensity.ATHLETE, 3, 45))).exerciseCalories();

            assertThat(athlete).isGreaterThan(low);
        }

        @Test
        void zeroWeight_givesNoExerciseCalories() {
            PersonalProfile p = maleOffice();
            p.setWeightKg(BigDecimal.ZERO);

            assertThat(calc.calculate(p, List.of(
                    activity(ActivityType.GYM, WorkoutIntensity.HIGH, 5, 60))).exerciseCalories())
                    .isEqualByComparingTo("0");
        }

        @Test
        void nullProfile_isRejected() {
            assertThatThrownBy(() -> calc.calculate(null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void missingBmrInputs_arePropagatedAsIllegalArgument() {
            PersonalProfile p = maleOffice();
            p.setHeightCm(null);

            assertThatThrownBy(() -> calc.calculate(p, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
