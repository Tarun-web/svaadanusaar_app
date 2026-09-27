import React, { useState } from 'react';
import { StyleSheet, View, Text, TextInput, TouchableOpacity, ActivityIndicator } from 'react-native';
import { COLORS, RADII, SPACING, SHADOWS } from '../../styles/theme';

export default function PersonalStep({ onSave, loading, initialData }) {
  const [formData, setFormData] = useState({
    gender: initialData?.gender || '',
    heightCm: initialData?.heightCm ? String(initialData.heightCm) : '',
    weightKg: initialData?.weightKg ? String(initialData.weightKg) : '',
    dateOfBirth: initialData?.dateOfBirth || '',
    occupation: initialData?.occupation || '',
    livingArrangement: initialData?.livingArrangement || 'FAMILY',
  });

  const updateField = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const isValid =
    formData.gender !== '' &&
    formData.dateOfBirth.trim().length >= 8 &&
    formData.heightCm.trim() !== '' &&
    !isNaN(parseFloat(formData.heightCm)) &&
    parseFloat(formData.heightCm) > 0 &&
    formData.weightKg.trim() !== '' &&
    !isNaN(parseFloat(formData.weightKg)) &&
    parseFloat(formData.weightKg) > 0;

  const handleNext = () => {
    if (!isValid) return;
    onSave({
      dateOfBirth: formData.dateOfBirth,
      gender: formData.gender,
      heightCm: parseFloat(formData.heightCm),
      weightKg: parseFloat(formData.weightKg),
      occupation: formData.occupation || 'OFFICE',
      livingArrangement: formData.livingArrangement || 'FAMILY',
      wakeUpTime: '06:30:00',
      sleepTime: '22:30:00',
      breakfastTime: '08:00:00',
      lunchTime: '13:00:00',
      dinnerTime: '20:00:00',
      nightShift: false,
    });
  };

  return (
    <View style={styles.stepCard}>
      <Text style={styles.stepTitle}>Personal Details</Text>
      <Text style={styles.stepDescription}>Tell us about yourself.</Text>

      <Text style={styles.inputLabel}>Gender *</Text>
      <View style={styles.chipRow}>
        {[
          { id: 'MALE', label: '♂ Male' },
          { id: 'FEMALE', label: '♀ Female' },
          { id: 'OTHER', label: 'Other' },
        ].map((g) => (
          <TouchableOpacity
            key={g.id}
            style={[styles.chip, formData.gender === g.id && styles.chipActive]}
            onPress={() => updateField('gender', g.id)}
          >
            <Text style={[styles.chipText, formData.gender === g.id && styles.chipTextActive]}>
              {g.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <View style={styles.fieldRow}>
        <View style={styles.flex1}>
          <Text style={styles.inputLabel}>Height (cm) *</Text>
          <TextInput
            style={styles.input}
            value={formData.heightCm}
            onChangeText={(val) => updateField('heightCm', val)}
            keyboardType="numeric"
            placeholder="e.g. 175"
          />
        </View>
        <View style={styles.spaceHorizontal} />
        <View style={styles.flex1}>
          <Text style={styles.inputLabel}>Current Weight (kg) *</Text>
          <TextInput
            style={styles.input}
            value={formData.weightKg}
            onChangeText={(val) => updateField('weightKg', val)}
            keyboardType="numeric"
            placeholder="e.g. 72"
          />
        </View>
      </View>

      <Text style={styles.inputLabel}>Date of Birth (YYYY-MM-DD) *</Text>
      <TextInput
        style={styles.input}
        value={formData.dateOfBirth}
        onChangeText={(val) => updateField('dateOfBirth', val)}
        placeholder="e.g. 1998-05-15"
      />

      <Text style={styles.inputLabel}>Occupation Type</Text>
      <View style={styles.chipGrid}>
        {[
          { id: 'OFFICE', label: '🏢 Office' },
          { id: 'WFH', label: '💻 Work From Home' },
          { id: 'HOSTEL_STUDENT', label: '🏫 Hostel Student' },
          { id: 'COLLEGE_STUDENT', label: '🎓 Day Scholar' },
          { id: 'HOME_MAKER', label: '🏠 Homemaker' },
          { id: 'RETIRED', label: '🌅 Retired' },
          { id: 'SHIFT_WORKER', label: '🌙 Shift Worker' },
          { id: 'MANUAL_LABOUR', label: '🏋️ Manual Labour' },
        ].map((item) => (
          <TouchableOpacity
            key={item.id}
            style={[styles.gridChip, formData.occupation === item.id && styles.chipActive]}
            onPress={() => updateField('occupation', item.id)}
          >
            <Text style={[styles.chipText, formData.occupation === item.id && styles.chipTextActive]}>
              {item.label}
            </Text>
          </TouchableOpacity>
        ))}
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
  fieldRow: { flexDirection: 'row', alignItems: 'center' },
  flex1: { flex: 1 },
  spaceHorizontal: { width: 12 },
  primaryButton: { backgroundColor: COLORS.accentGreen, borderRadius: RADII.button, paddingVertical: 16, alignItems: 'center', marginTop: 16, ...SHADOWS.card },
  buttonDisabled: { opacity: 0.6 },
  primaryButtonText: { color: COLORS.white, fontSize: 17, fontWeight: '700' },
});
