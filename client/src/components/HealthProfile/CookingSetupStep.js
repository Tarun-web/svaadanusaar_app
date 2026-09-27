import React, { useState } from 'react';
import { StyleSheet, View, Text, TextInput, TouchableOpacity, ActivityIndicator } from 'react-native';
import { COLORS, RADII, SPACING, SHADOWS } from '../../styles/theme';

export default function CookingSetupStep({ onSave, loading, initialData }) {
  const [formData, setFormData] = useState({
    cookingSkill: initialData?.cookingSkill || '',
    cookingTimeMinutes: initialData?.cookingTimeMinutes ? String(initialData.cookingTimeMinutes) : '',
    selectedEquipments: initialData?.equipments || initialData?.selectedEquipments || [],
    mealPrep: !!initialData?.mealPrep,
    budget: initialData?.budget || 'MEDIUM',
  });

  const updateField = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const toggleArrayItem = (list, field, item) => {
    const updated = list.includes(item) ? list.filter((i) => i !== item) : [...list, item];
    setFormData((prev) => ({ ...prev, [field]: updated }));
  };

  const isValid =
    formData.cookingSkill !== '' &&
    formData.cookingTimeMinutes.trim() !== '' &&
    !isNaN(parseInt(formData.cookingTimeMinutes, 10)) &&
    parseInt(formData.cookingTimeMinutes, 10) > 0;

  const handleNext = () => {
    if (!isValid) return;
    onSave({
      cookingSkill: formData.cookingSkill || 'BEGINNER',
      cookingTimeMinutes: parseInt(formData.cookingTimeMinutes, 10) || 30,
      equipments: formData.selectedEquipments,
      mealPrep: !!formData.mealPrep,
      budget: formData.budget || 'MEDIUM',
    });
  };

  return (
    <View style={styles.stepCard}>
      <Text style={styles.stepTitle}>Cooking & Kitchen Setup</Text>
      <Text style={styles.stepDescription}>How much time do you spend in the kitchen?</Text>

      <Text style={styles.inputLabel}>Cooking Skill Level *</Text>
      <View style={styles.chipRow}>
        {[
          { id: 'BEGINNER', label: '🍳 Beginner' },
          { id: 'INTERMEDIATE', label: '👨‍🍳 Intermediate' },
          { id: 'ADVANCED', label: '⭐ Pro Chef' },
        ].map((item) => (
          <TouchableOpacity
            key={item.id}
            style={[styles.chip, formData.cookingSkill === item.id && styles.chipActive]}
            onPress={() => updateField('cookingSkill', item.id)}
          >
            <Text style={[styles.chipText, formData.cookingSkill === item.id && styles.chipTextActive]}>
              {item.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.inputLabel}>Max Daily Cooking Time (Minutes) *</Text>
      <TextInput
        style={styles.input}
        value={formData.cookingTimeMinutes}
        onChangeText={(val) => updateField('cookingTimeMinutes', val)}
        keyboardType="numeric"
        placeholder="e.g. 30"
      />

      <Text style={styles.inputLabel}>Available Kitchen Appliances</Text>
      <View style={styles.chipGrid}>
        {[
          { id: 'GAS_STOVE', label: '🔥 Gas Stove' },
          { id: 'INDUCTION', label: '⚡ Induction' },
          { id: 'PRESSURE_COOKER', label: '🍲 Pressure Cooker' },
          { id: 'AIR_FRYER', label: '🍟 Air Fryer' },
          { id: 'MIXER_GRINDER', label: '🍹 Mixer Grinder' },
          { id: 'MICROWAVE', label: '📻 Microwave' },
        ].map((item) => {
          const selected = formData.selectedEquipments.includes(item.id);
          return (
            <TouchableOpacity
              key={item.id}
              style={[styles.gridChip, selected && styles.chipActive]}
              onPress={() => toggleArrayItem(formData.selectedEquipments, 'selectedEquipments', item.id)}
            >
              <Text style={[styles.chipText, selected && styles.chipTextActive]}>{item.label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

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
  primaryButton: { backgroundColor: COLORS.accentGreen, borderRadius: RADII.button, paddingVertical: 16, alignItems: 'center', marginTop: 16, ...SHADOWS.card },
  buttonDisabled: { opacity: 0.6 },
  primaryButtonText: { color: COLORS.white, fontSize: 17, fontWeight: '700' },
});
