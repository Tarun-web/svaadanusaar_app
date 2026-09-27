import React, { useState } from 'react';
import { StyleSheet, View, Text, TouchableOpacity, ActivityIndicator } from 'react-native';
import { COLORS, RADII, SPACING, SHADOWS } from '../../styles/theme';

export default function SupplementStep({ onSave, loading, initialData }) {
  const [formData, setFormData] = useState({
    openToSupplements: initialData?.openToSupplements !== undefined ? !!initialData.openToSupplements : true,
    currentSupplements: initialData?.currentSupplements || [],
  });

  const handleOpenToSupplementsChange = (val) => {
    setFormData((prev) => ({
      ...prev,
      openToSupplements: val,
      currentSupplements: val ? prev.currentSupplements : [],
    }));
  };

  const toggleSupplement = (suppId) => {
    setFormData((prev) => {
      const exists = prev.currentSupplements.includes(suppId);
      const updated = exists
        ? prev.currentSupplements.filter((s) => s !== suppId)
        : [...prev.currentSupplements, suppId];
      return { ...prev, currentSupplements: updated };
    });
  };

  const handleFinish = () => {
    onSave({
      openToSupplements: formData.openToSupplements,
      currentSupplements: formData.currentSupplements,
    });
  };

  const supplementList = [
    { id: 'WHEY', label: '🥛 Whey / Plant Protein' },
    { id: 'CREATINE', label: '⚡ Creatine Monohydrate' },
    { id: 'MULTIVITAMIN', label: '💊 Multivitamin' },
    { id: 'FISH_OIL', label: '🐟 Omega 3 / Fish Oil' },
    { id: 'VITAMIN_D', label: '☀️ Vitamin D3' },
    { id: 'VITAMIN_B12', label: '🌿 Vitamin B12' },
  ];

  return (
    <View style={styles.stepCard}>
      <Text style={styles.stepTitle}>Supplements & Stack</Text>
      <Text style={styles.stepDescription}>Do you currently take or prefer adding health supplements?</Text>

      <Text style={styles.inputLabel}>Do you like supplements?</Text>
      <View style={styles.yesNoRow}>
        <TouchableOpacity
          style={[styles.yesNoChip, formData.openToSupplements && styles.chipActive]}
          onPress={() => handleOpenToSupplementsChange(true)}
        >
          <Text style={[styles.chipText, formData.openToSupplements && styles.chipTextActive]}>
            Yes
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.yesNoChip, !formData.openToSupplements && styles.chipActive]}
          onPress={() => handleOpenToSupplementsChange(false)}
        >
          <Text style={[styles.chipText, !formData.openToSupplements && styles.chipTextActive]}>
            No
          </Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.inputLabel}>Current Supplement Usage (Select all that apply)</Text>
      <View style={styles.chipGrid}>
        {supplementList.map((item) => {
          const selected = formData.currentSupplements.includes(item.id);
          return (
            <TouchableOpacity
              key={item.id}
              style={[styles.gridChip, selected && styles.chipActive]}
              onPress={() => toggleSupplement(item.id)}
            >
              <Text style={[styles.chipText, selected && styles.chipTextActive]}>{item.label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <TouchableOpacity
        style={[styles.finishButton, loading && styles.buttonDisabled]}
        onPress={handleFinish}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color={COLORS.white} />
        ) : (
          <Text style={styles.finishButtonText}>Complete Setup & Unlock Diet 🚀</Text>
        )}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  stepCard: {
    backgroundColor: COLORS.white,
    borderRadius: RADII.card,
    padding: SPACING.space4,
    marginBottom: SPACING.space4,
    ...SHADOWS.card,
  },
  stepTitle: { fontSize: 22, fontWeight: '700', color: COLORS.starbucksGreen, marginBottom: 4 },
  stepDescription: { fontSize: 14, color: COLORS.textBlackSoft, marginBottom: SPACING.space3 },
  inputLabel: { fontSize: 14, fontWeight: '600', color: COLORS.houseGreen, marginTop: SPACING.space2, marginBottom: 8 },
  yesNoRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 12 },
  yesNoChip: {
    width: '48%',
    backgroundColor: COLORS.neutralCool,
    borderRadius: 12,
    paddingVertical: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    alignItems: 'center',
    justifyContent: 'center',
  },
  chipGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', marginBottom: 12 },
  gridChip: {
    width: '48%',
    backgroundColor: COLORS.neutralCool,
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 12,
    marginVertical: 4,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    alignItems: 'center',
    justifyContent: 'center',
  },
  chipActive: { backgroundColor: COLORS.lightGreen, borderColor: COLORS.accentGreen },
  chipText: { fontSize: 14, color: COLORS.textBlackSoft, fontWeight: '500', textAlign: 'center' },
  chipTextActive: { color: COLORS.houseGreen, fontWeight: '700' },
  finishButton: {
    backgroundColor: COLORS.starbucksGreen,
    borderRadius: RADII.button,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 16,
    ...SHADOWS.card,
  },
  buttonDisabled: { opacity: 0.6 },
  finishButtonText: { color: COLORS.white, fontSize: 17, fontWeight: '700' },
});
