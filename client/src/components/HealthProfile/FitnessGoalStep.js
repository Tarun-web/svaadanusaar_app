import React, { useState } from 'react';
import { StyleSheet, View, Text, TextInput, TouchableOpacity, ActivityIndicator } from 'react-native';
import { COLORS, RADII, SPACING, SHADOWS } from '../../styles/theme';

export default function FitnessGoalStep({ onSave, loading, initialData }) {
  const [formData, setFormData] = useState({
    primaryGoal: initialData?.primaryGoal || '',
    targetWeightKg: initialData?.targetWeightKg ? String(initialData.targetWeightKg) : '',
    weeklyWeightChangeKg: initialData?.weeklyWeightChangeKg ? String(initialData.weeklyWeightChangeKg) : '',
    includeCheatMeals: !!initialData?.includeCheatMeals,
    cheatMealsPerWeek: initialData?.cheatMealsPerWeek ? String(initialData.cheatMealsPerWeek) : '',
  });

  const updateField = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const isValid =
    formData.primaryGoal !== '' &&
    formData.targetWeightKg.trim() !== '' &&
    !isNaN(parseFloat(formData.targetWeightKg)) &&
    parseFloat(formData.targetWeightKg) > 0;

  const handleNext = () => {
    if (!isValid) return;
    onSave({
      primaryGoal: formData.primaryGoal || 'FAT_LOSS',
      targetWeightKg: parseFloat(formData.targetWeightKg) || 70,
      weeklyWeightChangeKg: parseFloat(formData.weeklyWeightChangeKg) || 0.5,
      includeCheatMeals: !!formData.includeCheatMeals,
      cheatMealsPerWeek: parseInt(formData.cheatMealsPerWeek, 10) || 0,
    });
  };

  return (
    <View style={styles.stepCard}>
      <Text style={styles.stepTitle}>Fitness & Target Goal</Text>
      <Text style={styles.stepDescription}>What do you want to achieve with Svaadanusaar?</Text>

      <Text style={styles.inputLabel}>Primary Goal *</Text>
      <View style={styles.chipGrid}>
        {[
          { id: 'FAT_LOSS', label: '🔥 Fat Loss' },
          { id: 'MUSCLE_GAIN', label: '💪 Muscle Gain' },
          { id: 'BODY_RECOMPOSITION', label: '⚡ Body Recomposition' },
          { id: 'WEIGHT_GAIN', label: '📈 Weight Gain' },
          { id: 'MAINTENANCE', label: '⚖️ Maintenance' },
          { id: 'GENERAL_HEALTH', label: '🥗 General Health' },
          { id: 'SPORTS_PERFORMANCE', label: '🏃 Sports Performance' },
        ].map((item) => (
          <TouchableOpacity
            key={item.id}
            style={[styles.gridChip, formData.primaryGoal === item.id && styles.chipActive]}
            onPress={() => updateField('primaryGoal', item.id)}
          >
            <Text style={[styles.chipText, formData.primaryGoal === item.id && styles.chipTextActive]}>
              {item.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <View style={styles.fieldRow}>
        <View style={styles.flex1}>
          <Text style={styles.inputLabel}>Target Weight (kg) *</Text>
          <TextInput
            style={styles.input}
            value={formData.targetWeightKg}
            onChangeText={(val) => updateField('targetWeightKg', val)}
            keyboardType="numeric"
            placeholder="e.g. 68"
          />
        </View>
        <View style={styles.spaceHorizontal} />
        <View style={styles.flex1}>
          <Text style={styles.inputLabel}>Weekly Pace (kg/wk)</Text>
          <TextInput
            style={styles.input}
            value={formData.weeklyWeightChangeKg}
            onChangeText={(val) => updateField('weeklyWeightChangeKg', val)}
            keyboardType="numeric"
            placeholder="e.g. 0.5"
          />
        </View>
      </View>

      <Text style={styles.inputLabel}>Do you want cheat meal? This is one of the best Svaadanusaar feature.</Text>
      <View style={styles.chipRow}>
        <TouchableOpacity
          style={[styles.chip, formData.includeCheatMeals && styles.chipActive]}
          onPress={() => updateField('includeCheatMeals', true)}
        >
          <Text style={[styles.chipText, formData.includeCheatMeals && styles.chipTextActive]}>
            Yes, why not?
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.chip, !formData.includeCheatMeals && styles.chipActive]}
          onPress={() => updateField('includeCheatMeals', false)}
        >
          <Text style={[styles.chipText, !formData.includeCheatMeals && styles.chipTextActive]}>
            No
          </Text>
        </TouchableOpacity>
      </View>

      {formData.includeCheatMeals && (
        <View style={{ marginTop: 8 }}>
          <Text style={styles.inputLabel}>Cheat Meals per Week</Text>
          <TextInput
            style={styles.input}
            value={formData.cheatMealsPerWeek}
            onChangeText={(val) => updateField('cheatMealsPerWeek', val)}
            keyboardType="numeric"
            placeholder="e.g. 1"
          />
        </View>
      )}

      <TouchableOpacity
        style={[styles.primaryButton, (!isValid || loading) && styles.buttonDisabled]}
        onPress={handleNext}
        disabled={!isValid || loading}
      >
        {loading ? <ActivityIndicator color={COLORS.white} /> : <Text style={styles.primaryButtonText}>Next →</Text>}
      </TouchableOpacity>

      {!isValid && null}
    </View>
  );
}

const styles = StyleSheet.create({
  stepCard: { backgroundColor: COLORS.white, borderRadius: RADII.card, padding: SPACING.space4, marginBottom: SPACING.space4, ...SHADOWS.card },
  stepTitle: { fontSize: 22, fontWeight: '700', color: COLORS.starbucksGreen, marginBottom: 4 },
  stepDescription: { fontSize: 14, color: COLORS.textBlackSoft, marginBottom: SPACING.space3 },
  inputLabel: { fontSize: 14, fontWeight: '600', color: COLORS.houseGreen, marginTop: SPACING.space2, marginBottom: 6 },
  input: { backgroundColor: COLORS.neutralCool, borderRadius: 8, borderWidth: 1, borderColor: '#E2E8F0', paddingHorizontal: 12, paddingVertical: 10, fontSize: 16, color: COLORS.textBlack, marginBottom: 8 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', marginBottom: 8 },
  chipGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', marginBottom: 8 },
  chip: { backgroundColor: COLORS.neutralCool, borderRadius: 20, paddingHorizontal: 16, paddingVertical: 8, marginRight: 8, marginBottom: 8, borderWidth: 1, borderColor: '#E2E8F0' },
  gridChip: { width: '48%', backgroundColor: COLORS.neutralCool, borderRadius: 12, paddingHorizontal: 10, paddingVertical: 12, marginVertical: 4, borderWidth: 1, borderColor: '#E2E8F0', alignItems: 'center', justifyContent: 'center' },
  chipActive: { backgroundColor: COLORS.lightGreen, borderColor: COLORS.accentGreen },
  chipText: { fontSize: 14, color: COLORS.textBlackSoft, fontWeight: '500', textAlign: 'center' },
  chipTextActive: { color: COLORS.houseGreen, fontWeight: '700' },
  fieldRow: { flexDirection: 'row', alignItems: 'center' },
  flex1: { flex: 1 },
  spaceHorizontal: { width: 12 },
  primaryButton: { backgroundColor: COLORS.accentGreen, borderRadius: RADII.button, paddingVertical: 16, alignItems: 'center', marginTop: 16, ...SHADOWS.card },
  buttonDisabled: { opacity: 0.6 },
  primaryButtonText: { color: COLORS.white, fontSize: 17, fontWeight: '700' },
});
