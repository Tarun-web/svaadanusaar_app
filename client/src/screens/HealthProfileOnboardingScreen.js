import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { COLORS, SHADOWS, RADII, SPACING } from '../styles/theme';
import { api } from '../services/api';

// Import 8 Modular Step Components
import PersonalStep from '../components/HealthProfile/PersonalStep';
import FitnessGoalStep from '../components/HealthProfile/FitnessGoalStep';
import NutritionPreferenceStep from '../components/HealthProfile/NutritionPreferenceStep';
import WorkoutRoutineStep from '../components/HealthProfile/WorkoutRoutineStep';
import MedicalStep from '../components/HealthProfile/MedicalStep';
import CookingSetupStep from '../components/HealthProfile/CookingSetupStep';
import LifestyleStep from '../components/HealthProfile/LifestyleStep';
import SupplementStep from '../components/HealthProfile/SupplementStep';

const TOTAL_STEPS = 8;

// --- Why We Ask Accordion Component with Typing Animation ---
function WhyWeAskAccordion({ text }) {
  const [expanded, setExpanded] = useState(false);
  const [typedText, setTypedText] = useState('');

  const cleanReason = text.startsWith('Why we ask: ') ? text.replace('Why we ask: ', '') : text;

  useEffect(() => {
    if (!expanded) {
      setTypedText('');
      return;
    }

    setTypedText('');
    let currentIndex = 0;
    const interval = setInterval(() => {
      currentIndex++;
      setTypedText(cleanReason.slice(0, currentIndex));
      if (currentIndex >= cleanReason.length) {
        clearInterval(interval);
      }
    }, 18);

    return () => clearInterval(interval);
  }, [expanded, text]);

  return (
    <View style={styles.accordionContainer}>
      <TouchableOpacity
        style={styles.accordionHeader}
        onPress={() => setExpanded(!expanded)}
        activeOpacity={0.8}
      >
        <View style={styles.accordionTitleRow}>
          <Text style={styles.whyBannerIcon}>💡</Text>
          <Text style={styles.accordionTitle}>Why we ask this?</Text>
        </View>
        <Text style={styles.chevron}>{expanded ? '▲' : '▼'}</Text>
      </TouchableOpacity>

      {expanded && (
        <View style={styles.accordionContent}>
          <Text style={styles.whyBannerText}>
            {typedText}
            {typedText.length < cleanReason.length && <Text style={styles.cursor}>|</Text>}
          </Text>
        </View>
      )}
    </View>
  );
}

export default function HealthProfileOnboardingScreen({ onComplete, onSignOut }) {
  const [currentStep, setCurrentStep] = useState(1);
  const [initializing, setInitializing] = useState(true);
  const [loading, setLoading] = useState(false);
  const [isFinished, setIsFinished] = useState(false);

  // Local state cache storing user responses for all 8 steps
  const [savedProfileData, setSavedProfileData] = useState({
    personal: null,
    goal: null,
    nutrition: null,
    workout: null,
    medical: null,
    cooking: null,
    lifestyle: null,
    supplement: null,
  });

  // Fetch full existing details on mount and restore progress & initialData
  useEffect(() => {
    async function initHealthProfile() {
      setInitializing(true);
      try {
        // 1. Fetch existing health profile details from backend
        const details = await api.getHealthProfileDetails();
        
        if (details) {
          setSavedProfileData({
            personal: details.personalProfile,
            goal: details.fitnessGoal,
            nutrition: details.nutritionPreference,
            workout: details.workoutProfile,
            medical: details.medicalProfile,
            cooking: details.cookingProfile,
            lifestyle: details.lifestylePreference,
            supplement: details.supplementProfile,
          });

          // Check if onboarding is 100% finished
          if (
            details.healthProfile?.onboardingCompleted ||
            (details.personalProfile &&
              details.fitnessGoal &&
              details.nutritionPreference &&
              details.workoutProfile &&
              details.medicalProfile &&
              details.cookingProfile &&
              details.lifestylePreference &&
              details.supplementProfile)
          ) {
            setIsFinished(true);
          } else {
            // Find the first uncompleted section to resume step on UI
            let resumeStep = 1;
            if (!details.personalProfile) {
              resumeStep = 1;
            } else if (!details.fitnessGoal) {
              resumeStep = 2;
            } else if (!details.nutritionPreference) {
              resumeStep = 3;
            } else if (!details.workoutProfile) {
              resumeStep = 4;
            } else if (!details.medicalProfile) {
              resumeStep = 5;
            } else if (!details.cookingProfile) {
              resumeStep = 6;
            } else if (!details.lifestylePreference) {
              resumeStep = 7;
            } else if (!details.supplementProfile) {
              resumeStep = 8;
            }

            console.log(`Existing Health Profile found. Resuming onboarding at Step ${resumeStep}`);
            setCurrentStep(resumeStep);
          }
        }
      } catch (err) {
        console.log('Health Profile not found for user. Creating initial Health Profile container...');
        await api.createHealthProfile().catch((createErr) => {
          console.log('HealthProfile container creation result:', createErr?.message || 'Created');
        });
      } finally {
        setInitializing(false);
      }
    }
    initHealthProfile();
  }, []);

  // Why Statements mapping per step
  const whyStatements = {
    1: 'Why we ask: Your age, gender, height, and weight are essential to calculate your accurate Basal Metabolic Rate (BMR) and daily energy needs.',
    2: 'Why we ask: Setting a precise target weight and pace helps us compute your exact daily caloric deficit or surplus safely.',
    3: 'Why we ask: Understanding your dietary preferences and meal frequencies allows us to design meal plans you will truly enjoy eating.',
    4: 'Why we ask: Aligning nutrient timing with your exercise schedule ensures optimum muscle recovery and peak energy during workouts.',
    5: 'Why we ask: Medical conditions and food allergies dictate critical safety rules to filter out harmful ingredients from your diet.',
    6: 'Why we ask: Matching recipes to your available kitchen equipment and daily time limits ensures meal preparation is realistic and stress-free.',
    7: 'Why we ask: Knowing your daily environment (hostel, office, travel) helps us suggest quick, portable, or dining-out meal options when needed.',
    8: 'Why we ask: Identifying your openness to supplements allows us to fill potential micronutrient gaps while staying within your preference.',
  };

  const handleSaveStep = async (stepNumber, profileData) => {
    setLoading(true);
    try {
      if (stepNumber === 1) {
        await api.updatePersonalProfile(profileData);
        setSavedProfileData((prev) => ({ ...prev, personal: profileData }));
      } else if (stepNumber === 2) {
        await api.updateFitnessGoalProfile(profileData);
        setSavedProfileData((prev) => ({ ...prev, goal: profileData }));
      } else if (stepNumber === 3) {
        await api.updateNutritionPreferenceProfile(profileData);
        setSavedProfileData((prev) => ({ ...prev, nutrition: profileData }));
      } else if (stepNumber === 4) {
        await api.updateWorkoutProfile(profileData);
        setSavedProfileData((prev) => ({ ...prev, workout: profileData }));
      } else if (stepNumber === 5) {
        await api.updateMedicalProfile(profileData);
        setSavedProfileData((prev) => ({ ...prev, medical: profileData }));
      } else if (stepNumber === 6) {
        await api.updateCookingProfile(profileData);
        setSavedProfileData((prev) => ({ ...prev, cooking: profileData }));
      } else if (stepNumber === 7) {
        await api.updateLifestylePreferenceProfile(profileData);
        setSavedProfileData((prev) => ({ ...prev, lifestyle: profileData }));
      } else if (stepNumber === 8) {
        await api.updateSupplementProfile(profileData);
        setSavedProfileData((prev) => ({ ...prev, supplement: profileData }));
        setIsFinished(true);
        setLoading(false);
        return;
      }

      if (currentStep < TOTAL_STEPS) {
        setCurrentStep((prev) => prev + 1);
      }
    } catch (err) {
      console.log(`Error saving step ${stepNumber}:`, err);
      if (currentStep < TOTAL_STEPS) {
        setCurrentStep((prev) => prev + 1);
      } else {
        setIsFinished(true);
      }
    } finally {
      setLoading(false);
    }
  };

  const handlePrevStep = () => {
    if (currentStep > 1) {
      setCurrentStep((prev) => prev - 1);
    }
  };

  const calculateEstimatedBmr = () => {
    const w = parseFloat(savedProfileData?.personal?.weightKg) || 70;
    const h = parseFloat(savedProfileData?.personal?.heightCm) || 170;
    const gender = savedProfileData?.personal?.gender || 'MALE';
    const a = 25;
    if (gender === 'MALE') {
      return Math.round(10 * w + 6.25 * h - 5 * a + 5);
    }
    return Math.round(10 * w + 6.25 * h - 5 * a - 161);
  };

  // --- Single Centered Screen Loader on Initial Load ---
  if (initializing) {
    return (
      <View style={styles.centeredLoaderContainer}>
        <ActivityIndicator size="large" color={COLORS.starbucksGreen} />
        <Text style={styles.initializingText}>Loading your profile...</Text>
      </View>
    );
  }

  const progressPercent = Math.round(((currentStep - 1) / TOTAL_STEPS) * 100);

  // --- Render 100% Finished Celebration Screen ---
  if (isFinished) {
    const bmr = calculateEstimatedBmr();
    const primaryGoalText = savedProfileData?.goal?.primaryGoal
      ? savedProfileData.goal.primaryGoal.replace('_', ' ')
      : 'General Health';
    const targetWeight = savedProfileData?.goal?.targetWeightKg || '70';
    const dietType = savedProfileData?.nutrition?.dietType || 'Balanced';
    const mealsPerDay = savedProfileData?.nutrition?.mealsPerDay || '3';

    return (
      <View style={styles.container}>
        <ScrollView contentContainerStyle={styles.celebrationScroll}>
          <View style={styles.celebrationHeader}>
            <View style={styles.badgeContainer}>
              <Text style={styles.badgeText}>🎉 100% HEALTH PROFILE COMPLETED</Text>
            </View>
            <Text style={styles.celebrationTitle}>You've done all of your work!</Text>
            <Text style={styles.celebrationSubtitle}>
              Now let us do the work to generate your tailor-made meal plan & AI nutrition roadmap.
            </Text>
          </View>

          <View style={styles.summaryCard}>
            <Text style={styles.summaryCardTitle}>✨ Your Personal Health Engine Profile</Text>

            <View style={styles.metricRow}>
              <View style={styles.metricBox}>
                <Text style={styles.metricLabel}>Estimated BMR</Text>
                <Text style={styles.metricValue}>{bmr} kcal/day</Text>
              </View>
              <View style={styles.metricBox}>
                <Text style={styles.metricLabel}>Target Goal</Text>
                <Text style={styles.metricValue}>
                  {primaryGoalText} ({targetWeight} kg)
                </Text>
              </View>
            </View>

            <View style={styles.metricRow}>
              <View style={styles.metricBox}>
                <Text style={styles.metricLabel}>Diet Philosophy</Text>
                <Text style={styles.metricValue}>{dietType}</Text>
              </View>
              <View style={styles.metricBox}>
                <Text style={styles.metricLabel}>Daily Meals</Text>
                <Text style={styles.metricValue}>{mealsPerDay} Meals / Day</Text>
              </View>
            </View>

            <View style={styles.divider} />

            <View style={styles.insightItem}>
              <Text style={styles.insightIcon}>🎯</Text>
              <Text style={styles.insightText}>
                Your custom macro breakdown and recipe recommendations have been synchronized with your profile.
              </Text>
            </View>
            <View style={styles.insightItem}>
              <Text style={styles.insightIcon}>⚡</Text>
              <Text style={styles.insightText}>
                Unlock full access to your personalized diet plans, AI chatbot, and grocery shopping lists!
              </Text>
            </View>
          </View>

          <TouchableOpacity style={styles.primaryButton} onPress={onComplete}>
            <Text style={styles.primaryButtonText}>Choose Subscription Plan 🚀</Text>
          </TouchableOpacity>
        </ScrollView>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      {/* Top Header & Step Progress Bar */}
      <View style={styles.topHeader}>
        <View style={styles.headerRow}>
          <TouchableOpacity
            onPress={currentStep === 1 ? onSignOut : handlePrevStep}
            style={styles.backButton}
          >
            <Text style={styles.backButtonText}>‹ Back</Text>
          </TouchableOpacity>
          <View style={styles.stepCounterContainer} pointerEvents="none">
            <Text style={styles.stepCounterText}>
              Step {currentStep} of {TOTAL_STEPS}
            </Text>
          </View>
          <Text style={styles.progressPercentText}>{progressPercent}%</Text>
        </View>

        <View style={styles.progressBarTrack}>
          <View style={[styles.progressBarFill, { width: `${progressPercent}%` }]} />
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
        {/* Why We Ask Accordion */}
        <WhyWeAskAccordion text={whyStatements[currentStep]} />

        {/* Modular Step Views with initialData pre-filling */}
        {currentStep === 1 && (
          <PersonalStep
            onSave={(data) => handleSaveStep(1, data)}
            loading={loading}
            initialData={savedProfileData.personal}
          />
        )}
        {currentStep === 2 && (
          <FitnessGoalStep
            onSave={(data) => handleSaveStep(2, data)}
            loading={loading}
            initialData={savedProfileData.goal}
          />
        )}
        {currentStep === 3 && (
          <NutritionPreferenceStep
            onSave={(data) => handleSaveStep(3, data)}
            loading={loading}
            initialData={savedProfileData.nutrition}
          />
        )}
        {currentStep === 4 && (
          <WorkoutRoutineStep
            onSave={(data) => handleSaveStep(4, data)}
            loading={loading}
            initialData={savedProfileData.workout}
          />
        )}
        {currentStep === 5 && (
          <MedicalStep
            onSave={(data) => handleSaveStep(5, data)}
            loading={loading}
            initialData={savedProfileData.medical}
          />
        )}
        {currentStep === 6 && (
          <CookingSetupStep
            onSave={(data) => handleSaveStep(6, data)}
            loading={loading}
            initialData={savedProfileData.cooking}
          />
        )}
        {currentStep === 7 && (
          <LifestyleStep
            onSave={(data) => handleSaveStep(7, data)}
            loading={loading}
            initialData={savedProfileData.lifestyle}
          />
        )}
        {currentStep === 8 && (
          <SupplementStep
            onSave={(data) => handleSaveStep(8, data)}
            loading={loading}
            initialData={savedProfileData.supplement}
          />
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.canvasWarm,
  },
  centeredLoaderContainer: {
    flex: 1,
    backgroundColor: COLORS.canvasWarm,
    justifyContent: 'center',
    alignItems: 'center',
  },
  initializingText: {
    marginTop: 12,
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.houseGreen,
  },
  topHeader: {
    backgroundColor: COLORS.white,
    paddingTop: 50,
    paddingHorizontal: SPACING.space3,
    paddingBottom: SPACING.space2,
    borderBottomWidth: 1,
    borderBottomColor: COLORS.canvasCeramic,
    ...SHADOWS.globalNav,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.space2,
    position: 'relative',
  },
  backButton: {
    paddingVertical: 4,
    paddingHorizontal: 8,
    zIndex: 2,
  },
  backButtonText: {
    color: COLORS.starbucksGreen,
    fontWeight: '600',
    fontSize: 16,
  },
  stepCounterContainer: {
    position: 'absolute',
    left: 0,
    right: 0,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepCounterText: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORS.houseGreen,
    textAlign: 'center',
  },
  progressPercentText: {
    fontSize: 14,
    fontWeight: '700',
    color: COLORS.accentGreen,
    zIndex: 2,
  },
  progressBarTrack: {
    height: 8,
    backgroundColor: COLORS.neutralCool,
    borderRadius: 4,
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: COLORS.accentGreen,
    borderRadius: 4,
  },
  scrollContent: {
    padding: SPACING.space3,
    paddingBottom: 150,
  },
  accordionContainer: {
    backgroundColor: COLORS.lightGreen,
    borderRadius: RADII.card,
    marginBottom: SPACING.space3,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#C6F6D5',
  },
  accordionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: SPACING.space3,
  },
  accordionTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  accordionTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORS.houseGreen,
  },
  chevron: {
    fontSize: 12,
    fontWeight: '700',
    color: COLORS.houseGreen,
  },
  accordionContent: {
    paddingHorizontal: SPACING.space3,
    paddingBottom: SPACING.space3,
  },
  whyBannerIcon: {
    fontSize: 18,
    marginRight: 8,
  },
  whyBannerText: {
    fontSize: 14,
    color: COLORS.houseGreen,
    fontWeight: '500',
    lineHeight: 20,
  },
  cursor: {
    fontWeight: '800',
    color: COLORS.accentGreen,
  },
  primaryButton: {
    backgroundColor: COLORS.starbucksGreen,
    borderRadius: RADII.button,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 8,
    ...SHADOWS.card,
  },
  primaryButtonText: {
    color: COLORS.white,
    fontSize: 17,
    fontWeight: '700',
  },
  celebrationScroll: {
    padding: SPACING.space4,
    paddingTop: 60,
  },
  celebrationHeader: {
    alignItems: 'center',
    marginBottom: SPACING.space4,
  },
  badgeContainer: {
    backgroundColor: COLORS.lightGreen,
    paddingHorizontal: 16,
    paddingVertical: 6,
    borderRadius: 20,
    marginBottom: 12,
  },
  badgeText: {
    color: COLORS.houseGreen,
    fontWeight: '700',
    fontSize: 13,
  },
  celebrationTitle: {
    fontSize: 28,
    fontWeight: '800',
    color: COLORS.starbucksGreen,
    textAlign: 'center',
    marginBottom: 8,
  },
  celebrationSubtitle: {
    fontSize: 16,
    color: COLORS.textBlackSoft,
    textAlign: 'center',
    lineHeight: 22,
  },
  summaryCard: {
    backgroundColor: COLORS.white,
    borderRadius: RADII.card,
    padding: SPACING.space4,
    marginBottom: SPACING.space5,
    ...SHADOWS.frap,
  },
  summaryCardTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: COLORS.houseGreen,
    marginBottom: SPACING.space3,
  },
  metricRow: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  metricBox: {
    flex: 1,
    backgroundColor: COLORS.neutralCool,
    padding: 12,
    borderRadius: 10,
    marginRight: 6,
  },
  metricLabel: {
    fontSize: 12,
    color: COLORS.textBlackSoft,
    marginBottom: 4,
  },
  metricValue: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORS.starbucksGreen,
  },
  divider: {
    height: 1,
    backgroundColor: COLORS.canvasCeramic,
    marginVertical: 12,
  },
  insightItem: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: 10,
  },
  insightIcon: {
    fontSize: 18,
    marginRight: 8,
  },
  insightText: {
    flex: 1,
    fontSize: 14,
    color: COLORS.textBlack,
    lineHeight: 20,
  },
});
