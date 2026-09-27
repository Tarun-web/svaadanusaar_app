import React, { useState } from 'react';
import { StyleSheet, View, Text, TouchableOpacity, ActivityIndicator } from 'react-native';
import { COLORS, RADII, SPACING, SHADOWS } from '../../styles/theme';

export default function MedicalStep({ onSave, loading, initialData }) {
  const [formData, setFormData] = useState({
    medicalConditions: initialData?.medicalConditions || [],
    foodAllergies: initialData?.foodAllergies || [],
    pregnancyStatus: initialData?.pregnancyStatus || 'NOT_APPLICABLE',
  });

  const toggleArrayItem = (list, field, item) => {
    const updated = list.includes(item) ? list.filter((i) => i !== item) : [...list, item];
    setFormData((prev) => ({ ...prev, [field]: updated }));
  };

  const handleNext = () => {
    onSave({
      medicalConditions: formData.medicalConditions,
      foodAllergies: formData.foodAllergies,
      pregnancyStatus: formData.pregnancyStatus || 'NOT_APPLICABLE',
    });
  };

  return (
    <View style={styles.stepCard}>
      <Text style={styles.stepTitle}>Health & Medical Safety</Text>
      <Text style={styles.stepDescription}>Please select any medical conditions or food allergies.</Text>

      <Text style={styles.inputLabel}>Medical Conditions</Text>
      <View style={styles.chipGrid}>
        {[
          { id: 'DIABETES', label: '🩺 Diabetes' },
          { id: 'HYPERTENSION', label: '❤️ High BP' },
          { id: 'THYROID', label: '🦋 Thyroid' },
          { id: 'PCOS', label: '🌸 PCOS / PCOD' },
          { id: 'CHOLESTEROL', label: '🩸 High Cholesterol' },
        ].map((item) => {
          const selected = formData.medicalConditions.includes(item.id);
          return (
            <TouchableOpacity
              key={item.id}
              style={[styles.gridChip, selected && styles.chipActive]}
              onPress={() => toggleArrayItem(formData.medicalConditions, 'medicalConditions', item.id)}
            >
              <Text style={[styles.chipText, selected && styles.chipTextActive]}>{item.label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <Text style={styles.inputLabel}>Food Allergies & Intolerances</Text>
      <View style={styles.chipGrid}>
        {[
          { id: 'LACTOSE', label: '🥛 Lactose' },
          { id: 'PEANUT', label: '🥜 Peanuts' },
          { id: 'GLUTEN', label: '🌾 Gluten' },
          { id: 'SOY', label: '🫘 Soy' },
          { id: 'EGG', label: '🥚 Egg' },
          { id: 'FISH', label: '🐟 Fish' },
          { id: 'SHELLFISH', label: '🦐 Shellfish' },
        ].map((item) => {
          const selected = formData.foodAllergies.includes(item.id);
          return (
            <TouchableOpacity
              key={item.id}
              style={[styles.gridChip, selected && styles.chipActive]}
              onPress={() => toggleArrayItem(formData.foodAllergies, 'foodAllergies', item.id)}
            >
              <Text style={[styles.chipText, selected && styles.chipTextActive]}>{item.label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <TouchableOpacity
        style={[styles.primaryButton, loading && styles.buttonDisabled]}
        onPress={handleNext}
        disabled={loading}
      >
        {loading ? <ActivityIndicator color={COLORS.white} /> : <Text style={styles.primaryButtonText}>Next →</Text>}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  stepCard: { backgroundColor: COLORS.white, borderRadius: RADII.card, padding: SPACING.space4, marginBottom: SPACING.space4, ...SHADOWS.card },
  stepTitle: { fontSize: 22, fontWeight: '700', color: COLORS.starbucksGreen, marginBottom: 4 },
  stepDescription: { fontSize: 14, color: COLORS.textBlackSoft, marginBottom: SPACING.space3 },
  inputLabel: { fontSize: 14, fontWeight: '600', color: COLORS.houseGreen, marginTop: SPACING.space2, marginBottom: 6 },
  chipGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', marginBottom: 8 },
  gridChip: { width: '48%', backgroundColor: COLORS.neutralCool, borderRadius: 12, paddingHorizontal: 10, paddingVertical: 12, marginVertical: 4, borderWidth: 1, borderColor: '#E2E8F0', alignItems: 'center', justifyContent: 'center' },
  chipActive: { backgroundColor: COLORS.lightGreen, borderColor: COLORS.accentGreen },
  chipText: { fontSize: 14, color: COLORS.textBlackSoft, fontWeight: '500', textAlign: 'center' },
  chipTextActive: { color: COLORS.houseGreen, fontWeight: '700' },
  primaryButton: { backgroundColor: COLORS.accentGreen, borderRadius: RADII.button, paddingVertical: 16, alignItems: 'center', marginTop: 16, ...SHADOWS.card },
  buttonDisabled: { opacity: 0.6 },
  primaryButtonText: { color: COLORS.white, fontSize: 17, fontWeight: '700' },
});
