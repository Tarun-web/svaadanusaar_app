import React, { useState } from 'react';
import { StyleSheet, View, Text, TouchableOpacity, ActivityIndicator } from 'react-native';
import { COLORS, RADII, SPACING, SHADOWS } from '../../styles/theme';

export default function LifestyleStep({ onSave, loading, initialData }) {
  const [formData, setFormData] = useState({
    hosteller: !!initialData?.hosteller,
    officeLunchAvailable: !!initialData?.officeLunchAvailable,
    travelsFrequently: !!initialData?.travelsFrequently,
    foodDeliveryAvailable: !!initialData?.foodDeliveryAvailable,
  });

  const toggleField = (field) => {
    setFormData((prev) => ({ ...prev, [field]: !prev[field] }));
  };

  const handleNext = () => {
    onSave({
      hosteller: formData.hosteller,
      officeLunchAvailable: formData.officeLunchAvailable,
      travelsFrequently: formData.travelsFrequently,
      foodDeliveryAvailable: formData.foodDeliveryAvailable,
    });
  };

  const lifestyleOptions = [
    {
      key: 'hosteller',
      title: '🏠 Hostel / PG Resident',
      desc: 'Shared kitchen or mess food environment',
    },
    {
      key: 'officeLunchAvailable',
      title: '🏢 Office Lunch / Canteen',
      desc: 'Provided or catered lunch at workplace',
    },
    {
      key: 'travelsFrequently',
      title: '✈️ Frequent Traveler',
      desc: 'Travels multi-city regularly for work or leisure',
    },
    {
      key: 'foodDeliveryAvailable',
      title: '🛵 Food Delivery Access',
      desc: 'Access to Swiggy / Zomato / healthy tiffin service',
    },
  ];

  return (
    <View style={styles.stepCard}>
      <Text style={styles.stepTitle}>Lifestyle Preferences</Text>
      <Text style={styles.stepDescription}>Help us adapt meals to your daily routine</Text>

      {lifestyleOptions.map((item) => {
        const isActive = formData[item.key];
        return (
          <TouchableOpacity
            key={item.key}
            style={[styles.toggleCard, isActive && styles.toggleCardActive]}
            onPress={() => toggleField(item.key)}
            activeOpacity={0.8}
          >
            <View style={{ flex: 1 }}>
              <Text style={[styles.toggleTitle, isActive && styles.toggleTitleActive]}>{item.title}</Text>
              <Text style={styles.toggleDesc}>{item.desc}</Text>
            </View>
            <View style={[styles.switchBadge, isActive && styles.switchBadgeActive]}>
              <Text style={[styles.switchBadgeText, isActive && styles.switchBadgeTextActive]}>
                {isActive ? 'YES' : 'NO'}
              </Text>
            </View>
          </TouchableOpacity>
        );
      })}

      <TouchableOpacity
        style={[styles.primaryButton, loading && styles.buttonDisabled]}
        onPress={handleNext}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color={COLORS.white} />
        ) : (
          <Text style={styles.primaryButtonText}>Next →</Text>
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
  toggleCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: COLORS.neutralCool,
    borderRadius: 14,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1.5,
    borderColor: '#E2E8F0',
  },
  toggleCardActive: {
    backgroundColor: COLORS.lightGreen,
    borderColor: COLORS.accentGreen,
  },
  toggleTitle: { fontSize: 15, fontWeight: '600', color: COLORS.textBlack, marginBottom: 2 },
  toggleTitleActive: { color: COLORS.houseGreen, fontWeight: '700' },
  toggleDesc: { fontSize: 12, color: COLORS.textBlackSoft },
  switchBadge: {
    backgroundColor: '#E2E8F0',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
    marginLeft: 8,
  },
  switchBadgeActive: {
    backgroundColor: COLORS.accentGreen,
  },
  switchBadgeText: { fontSize: 12, fontWeight: '700', color: COLORS.textBlackSoft },
  switchBadgeTextActive: { color: COLORS.white },
  primaryButton: {
    backgroundColor: COLORS.accentGreen,
    borderRadius: RADII.button,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 16,
    ...SHADOWS.card,
  },
  buttonDisabled: { opacity: 0.6 },
  primaryButtonText: { color: COLORS.white, fontSize: 17, fontWeight: '700' },
});
