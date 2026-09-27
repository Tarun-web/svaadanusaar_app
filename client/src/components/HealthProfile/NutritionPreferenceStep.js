import React, { useState } from 'react';
import { StyleSheet, View, Text, TouchableOpacity, ActivityIndicator } from 'react-native';
import { COLORS, RADII, SPACING, SHADOWS } from '../../styles/theme';

export default function NutritionPreferenceStep({ onSave, loading, initialData }) {
  const [formData, setFormData] = useState({
    dietType: initialData?.dietType || '',
    mealsPerDay: initialData?.mealsPerDay ? String(initialData.mealsPerDay) : '',
    spicePreference: initialData?.spicePreference || 'MEDIUM',
    dairyPreference: initialData?.dairyPreference || 'YES',
    consumesEggs: !!initialData?.consumesEggs,
    selectedCuisines: initialData?.preferredCuisines || initialData?.selectedCuisines || [],
  });

  const updateField = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const toggleArrayItem = (list, field, item) => {
    const updated = list.includes(item) ? list.filter((i) => i !== item) : [...list, item];
    setFormData((prev) => ({ ...prev, [field]: updated }));
  };

  const isValid = formData.dietType !== '' && formData.mealsPerDay !== '';

  const handleNext = () => {
    if (!isValid) return;
    onSave({
      dietType: formData.dietType || 'VEG',
      mealsPerDay: parseInt(formData.mealsPerDay, 10) || 3,
      preferredMealSize: 'MEDIUM',
      spicePreference: formData.spicePreference || 'MEDIUM',
      dairyPreference: formData.dairyPreference || 'YES',
      consumesEggs: !!formData.consumesEggs,
      foodVarietyPreference: 'MEDIUM',
      preferredCuisines: formData.selectedCuisines.length > 0 ? formData.selectedCuisines : ['NORTH_INDIAN'],
    });
  };

  return (
    <View style={styles.stepCard}>
      <Text style={styles.stepTitle}>Dietary Preferences</Text>
      <Text style={styles.stepDescription}>Tell us your dietary pattern and tastes.</Text>

      <Text style={styles.inputLabel}>Diet Type *</Text>
      <View style={styles.chipGrid}>
        {[
          { id: 'VEG', label: '🥦 Vegetarian' },
          { id: 'NON_VEG', label: '🍗 Non-Vegetarian' },
          { id: 'EGGETARIAN', label: '🥚 Eggetarian' },
          { id: 'VEGAN', label: '🌱 Vegan' },
          { id: 'JAIN', label: '🙏 Jain' },
          { id: 'SATVIK', label: '✨ Satvik' },
        ].map((item) => (
          <TouchableOpacity
            key={item.id}
            style={[styles.gridChip, formData.dietType === item.id && styles.chipActive]}
            onPress={() => updateField('dietType', item.id)}
          >
            <Text style={[styles.chipText, formData.dietType === item.id && styles.chipTextActive]}>
              {item.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.inputLabel}>Preferred Meals Per Day *</Text>
      <View style={styles.chipRow}>
        {['2', '3', '4', '5'].map((num) => (
          <TouchableOpacity
            key={num}
            style={[styles.chip, formData.mealsPerDay === num && styles.chipActive]}
            onPress={() => updateField('mealsPerDay', num)}
          >
            <Text style={[styles.chipText, formData.mealsPerDay === num && styles.chipTextActive]}>
              {num} Meals
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.inputLabel}>Spice Level</Text>
      <View style={styles.chipRow}>
        {['MILD', 'MEDIUM', 'SPICY'].map((lvl) => (
          <TouchableOpacity
            key={lvl}
            style={[styles.chip, formData.spicePreference === lvl && styles.chipActive]}
            onPress={() => updateField('spicePreference', lvl)}
          >
            <Text style={[styles.chipText, formData.spicePreference === lvl && styles.chipTextActive]}>
              {lvl === 'MILD' ? '🌶️ Mild' : lvl === 'MEDIUM' ? '🌶️🌶️ Medium' : '🌶️🌶️🌶️ Spicy'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.inputLabel}>Dairy Preference</Text>
      <View style={styles.chipRow}>
        {[
          { id: 'YES', label: '🥛 Yes (Milk, Curd, Paneer)' },
          { id: 'LIMITED', label: '🧈 Limited' },
          { id: 'NO', label: '❌ No Dairy' },
        ].map((item) => (
          <TouchableOpacity
            key={item.id}
            style={[styles.chip, formData.dairyPreference === item.id && styles.chipActive]}
            onPress={() => updateField('dairyPreference', item.id)}
          >
            <Text style={[styles.chipText, formData.dairyPreference === item.id && styles.chipTextActive]}>
              {item.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.inputLabel}>Favorite Regional Cuisines</Text>
      <View style={styles.chipGrid}>
        {[
          { id: 'NORTH_INDIAN', label: '🫓 North Indian' },
          { id: 'SOUTH_INDIAN', label: '🍚 South Indian' },
          { id: 'PUNJABI', label: '🫓 Punjabi' },
          { id: 'GUJARATI', label: '🍲 Gujarati' },
          { id: 'MAHARASHTRIAN', label: '🥘 Maharashtrian' },
          { id: 'BENGALI', label: '🐟 Bengali' },
          { id: 'RAJASTHANI', label: '🌶️ Rajasthani' },
          { id: 'HYDERABADI', label: '🍲 Hyderabadi' },
          { id: 'MIXED', label: '🍱 Mixed / All' },
        ].map((item) => {
          const selected = formData.selectedCuisines.includes(item.id);
          return (
            <TouchableOpacity
              key={item.id}
              style={[styles.gridChip, selected && styles.chipActive]}
              onPress={() => toggleArrayItem(formData.selectedCuisines, 'selectedCuisines', item.id)}
            >
              <Text style={[styles.chipText, selected && styles.chipTextActive]}>
                {item.label}
              </Text>
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
