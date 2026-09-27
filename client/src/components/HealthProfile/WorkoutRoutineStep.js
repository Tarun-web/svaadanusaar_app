import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  Modal,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { COLORS, RADII, SPACING, SHADOWS } from '../../styles/theme';

export default function WorkoutRoutineStep({ onSave, loading, initialData }) {
  const [formData, setFormData] = useState({
    preferredWorkoutTime: initialData?.preferredWorkoutTime || '07:00:00',
    preferredRestDay: initialData?.preferredRestDay || 'SUNDAY',
    includePreWorkoutMeal: !!initialData?.includePreWorkoutMeal,
    includePostWorkoutMeal: !!initialData?.includePostWorkoutMeal,
  });

  // Clock Modal State
  const [clockModalVisible, setClockModalVisible] = useState(false);
  const [selectedHour, setSelectedHour] = useState('07');
  const [selectedMinute, setSelectedMinute] = useState('00');
  const [selectedPeriod, setSelectedPeriod] = useState('AM');

  const updateField = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  // Convert "HH:mm:ss" to 12-hour display format (e.g. "07:00:00" -> "07:00 AM")
  const formatDisplayTime = (timeStr) => {
    if (!timeStr) return 'Select Workout Time ⏰';
    const parts = timeStr.split(':');
    let h = parseInt(parts[0], 10) || 7;
    const m = parts[1] || '00';
    const period = h >= 12 ? 'PM' : 'AM';
    h = h % 12;
    if (h === 0) h = 12;
    const hStr = h < 10 ? `0${h}` : `${h}`;
    return `${hStr}:${m} ${period}`;
  };

  // Confirm Clock Picker Selection
  const handleConfirmClock = () => {
    let h = parseInt(selectedHour, 10);
    if (selectedPeriod === 'PM' && h < 12) h += 12;
    if (selectedPeriod === 'AM' && h === 12) h = 0;
    const hStr = h < 10 ? `0${h}` : `${h}`;
    const isoTime = `${hStr}:${selectedMinute}:00`;
    updateField('preferredWorkoutTime', isoTime);
    setClockModalVisible(false);
  };

  const isValid = formData.preferredWorkoutTime !== '' && formData.preferredRestDay !== '';

  const handleNext = () => {
    if (!isValid) return;
    onSave({
      preferredWorkoutTime: formData.preferredWorkoutTime,
      includePreWorkoutMeal: !!formData.includePreWorkoutMeal,
      includePostWorkoutMeal: !!formData.includePostWorkoutMeal,
      preferredRestDay: formData.preferredRestDay,
    });
  };

  const hoursList = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'];
  const minutesList = ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'];

  const restDayOptions = [
    { id: 'SUNDAY', label: '📅 Sunday' },
    { id: 'SATURDAY', label: '📅 Saturday' },
    { id: 'MONDAY', label: '📅 Monday' },
    { id: 'TUESDAY', label: '📅 Tuesday' },
    { id: 'WEDNESDAY', label: '📅 Wednesday' },
    { id: 'THURSDAY', label: '📅 Thursday' },
    { id: 'FRIDAY', label: '📅 Friday' },
  ];

  return (
    <View style={styles.stepCard}>
      <Text style={styles.stepTitle}>Workout & Activity Timing</Text>
      <Text style={styles.stepDescription}>Sync your nutrient intake with your exercise schedule.</Text>

      {/* Clock Time Selector Card */}
      <Text style={styles.inputLabel}>Preferred Workout Time *</Text>
      <TouchableOpacity
        style={styles.clockDisplayCard}
        onPress={() => setClockModalVisible(true)}
        activeOpacity={0.8}
      >
        <View style={styles.clockCardLeft}>
          <Text style={styles.clockIcon}>⏰</Text>
          <View>
            <Text style={styles.clockDisplayTime}>{formatDisplayTime(formData.preferredWorkoutTime)}</Text>
            <Text style={styles.clockSubtitle}>Tap to open clock time picker</Text>
          </View>
        </View>
        <View style={styles.clockChangeBadge}>
          <Text style={styles.clockChangeText}>Change 🕒</Text>
        </View>
      </TouchableOpacity>

      {/* Weekly Rest Day */}
      <Text style={styles.inputLabel}>Weekly Rest Day *</Text>
      <View style={styles.chipGrid}>
        {restDayOptions.map((item) => (
          <TouchableOpacity
            key={item.id}
            style={[styles.gridChip, formData.preferredRestDay === item.id && styles.chipActive]}
            onPress={() => updateField('preferredRestDay', item.id)}
          >
            <Text style={[styles.chipText, formData.preferredRestDay === item.id && styles.chipTextActive]}>
              {item.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Pre / Post Workout Toggles */}
      <Text style={styles.inputLabel}>Pre / Post Workout Meals</Text>
      <TouchableOpacity
        style={[styles.toggleRow, formData.includePreWorkoutMeal && styles.toggleRowActive]}
        onPress={() => updateField('includePreWorkoutMeal', !formData.includePreWorkoutMeal)}
      >
        <Text style={styles.toggleText}>⚡ Pre-workout energy snack</Text>
        <Text style={styles.toggleCheck}>{formData.includePreWorkoutMeal ? '✓' : '+'}</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.toggleRow, formData.includePostWorkoutMeal && styles.toggleRowActive]}
        onPress={() => updateField('includePostWorkoutMeal', !formData.includePostWorkoutMeal)}
      >
        <Text style={styles.toggleText}>🥤 Post-workout recovery shake/meal</Text>
        <Text style={styles.toggleCheck}>{formData.includePostWorkoutMeal ? '✓' : '+'}</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.primaryButton, (!isValid || loading) && styles.buttonDisabled]}
        onPress={handleNext}
        disabled={!isValid || loading}
      >
        {loading ? <ActivityIndicator color={COLORS.white} /> : <Text style={styles.primaryButtonText}>Next →</Text>}
      </TouchableOpacity>

      {/* ================= CLOCK TIME PICKER MODAL ================= */}
      <Modal
        visible={clockModalVisible}
        transparent={true}
        animationType="slide"
        onRequestClose={() => setClockModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>⏰ Select Workout Time</Text>
            <Text style={styles.modalSubtitle}>Pick your daily exercise hour & minute</Text>

            {/* Time Preview Header */}
            <View style={styles.timePreviewBox}>
              <Text style={styles.timePreviewText}>
                {selectedHour}:{selectedMinute} {selectedPeriod}
              </Text>
            </View>

            <ScrollView style={{ maxHeight: 320 }} showsVerticalScrollIndicator={false}>
              {/* Hour Selection Grid */}
              <Text style={styles.pickerSectionLabel}>Select Hour</Text>
              <View style={styles.pickerGrid}>
                {hoursList.map((h) => (
                  <TouchableOpacity
                    key={h}
                    style={[styles.pickerChip, selectedHour === h && styles.pickerChipActive]}
                    onPress={() => setSelectedHour(h)}
                  >
                    <Text style={[styles.pickerChipText, selectedHour === h && styles.pickerChipTextActive]}>
                      {h}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              {/* Minute Selection Grid */}
              <Text style={styles.pickerSectionLabel}>Select Minute</Text>
              <View style={styles.pickerGrid}>
                {minutesList.map((m) => (
                  <TouchableOpacity
                    key={m}
                    style={[styles.pickerChip, selectedMinute === m && styles.pickerChipActive]}
                    onPress={() => setSelectedMinute(m)}
                  >
                    <Text style={[styles.pickerChipText, selectedMinute === m && styles.pickerChipTextActive]}>
                      :{m}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              {/* AM / PM Selection */}
              <Text style={styles.pickerSectionLabel}>Select Period</Text>
              <View style={styles.periodRow}>
                {['AM', 'PM'].map((p) => (
                  <TouchableOpacity
                    key={p}
                    style={[styles.periodChip, selectedPeriod === p && styles.periodChipActive]}
                    onPress={() => setSelectedPeriod(p)}
                  >
                    <Text style={[styles.periodChipText, selectedPeriod === p && styles.periodChipTextActive]}>
                      {p === 'AM' ? '☀️ AM' : '🌙 PM'}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </ScrollView>

            <TouchableOpacity style={styles.confirmModalButton} onPress={handleConfirmClock}>
              <Text style={styles.confirmModalButtonText}>Set Workout Time ⏰</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.closeModalButton} onPress={() => setClockModalVisible(false)}>
              <Text style={styles.closeModalButtonText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  stepCard: { backgroundColor: COLORS.white, borderRadius: RADII.card, padding: SPACING.space4, marginBottom: SPACING.space4, ...SHADOWS.card },
  stepTitle: { fontSize: 22, fontWeight: '700', color: COLORS.starbucksGreen, marginBottom: 4 },
  stepDescription: { fontSize: 14, color: COLORS.textBlackSoft, marginBottom: SPACING.space3 },
  inputLabel: { fontSize: 14, fontWeight: '600', color: COLORS.houseGreen, marginTop: SPACING.space2, marginBottom: 6 },

  // Clock Card Styles
  clockDisplayCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: COLORS.lightGreen,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1.5,
    borderColor: COLORS.accentGreen,
    marginBottom: 10,
  },
  clockCardLeft: { flexDirection: 'row', alignItems: 'center' },
  clockIcon: { fontSize: 24, marginRight: 12 },
  clockDisplayTime: { fontSize: 18, fontWeight: '800', color: COLORS.starbucksGreen },
  clockSubtitle: { fontSize: 12, color: COLORS.textBlackSoft },
  clockChangeBadge: { backgroundColor: COLORS.accentGreen, paddingHorizontal: 12, paddingVertical: 6, borderRadius: 12 },
  clockChangeText: { fontSize: 12, fontWeight: '700', color: COLORS.white },

  chipGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', marginBottom: 8 },
  gridChip: { width: '48%', backgroundColor: COLORS.neutralCool, borderRadius: 12, paddingHorizontal: 10, paddingVertical: 12, marginVertical: 4, borderWidth: 1, borderColor: '#E2E8F0', alignItems: 'center', justifyContent: 'center' },
  chipActive: { backgroundColor: COLORS.lightGreen, borderColor: COLORS.accentGreen },
  chipText: { fontSize: 14, color: COLORS.textBlackSoft, fontWeight: '500', textAlign: 'center' },
  chipTextActive: { color: COLORS.houseGreen, fontWeight: '700' },
  toggleRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: COLORS.neutralCool, padding: 14, borderRadius: 10, marginBottom: 8, borderWidth: 1, borderColor: '#E2E8F0' },
  toggleRowActive: { backgroundColor: COLORS.lightGreen, borderColor: COLORS.accentGreen },
  toggleText: { fontSize: 15, fontWeight: '600', color: COLORS.houseGreen },
  toggleCheck: { fontSize: 18, fontWeight: '700', color: COLORS.accentGreen },
  primaryButton: { backgroundColor: COLORS.accentGreen, borderRadius: RADII.button, paddingVertical: 16, alignItems: 'center', marginTop: 16, ...SHADOWS.card },
  buttonDisabled: { opacity: 0.6 },
  primaryButtonText: { color: COLORS.white, fontSize: 17, fontWeight: '700' },

  // Modal Styles
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: SPACING.space3,
  },
  modalCard: {
    width: '100%',
    maxHeight: '80%',
    backgroundColor: COLORS.white,
    borderRadius: 20,
    padding: SPACING.space4,
    ...SHADOWS.card,
  },
  modalTitle: { fontSize: 20, fontWeight: '700', color: COLORS.starbucksGreen, textAlign: 'center' },
  modalSubtitle: { fontSize: 13, color: COLORS.textBlackSoft, textAlign: 'center', marginBottom: 12 },
  timePreviewBox: {
    backgroundColor: COLORS.lightGreen,
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 12,
    borderWidth: 1,
    borderColor: COLORS.accentGreen,
  },
  timePreviewText: { fontSize: 24, fontWeight: '800', color: COLORS.starbucksGreen },
  pickerSectionLabel: { fontSize: 13, fontWeight: '700', color: COLORS.houseGreen, marginTop: 8, marginBottom: 6 },
  pickerGrid: { flexDirection: 'row', flexWrap: 'wrap', marginHorizontal: -4 },
  pickerChip: {
    width: '23%',
    backgroundColor: COLORS.neutralCool,
    borderRadius: 10,
    paddingVertical: 10,
    margin: '1%',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  pickerChipActive: { backgroundColor: COLORS.accentGreen, borderColor: COLORS.accentGreen },
  pickerChipText: { fontSize: 14, fontWeight: '600', color: COLORS.textBlackSoft },
  pickerChipTextActive: { color: COLORS.white },
  periodRow: { flexDirection: 'row', justifyContent: 'space-between' },
  periodChip: { flex: 1, backgroundColor: COLORS.neutralCool, borderRadius: 10, paddingVertical: 12, marginHorizontal: 4, alignItems: 'center', borderWidth: 1, borderColor: '#E2E8F0' },
  periodChipActive: { backgroundColor: COLORS.starbucksGreen, borderColor: COLORS.starbucksGreen },
  periodChipText: { fontSize: 15, fontWeight: '700', color: COLORS.textBlackSoft },
  periodChipTextActive: { color: COLORS.white },
  confirmModalButton: {
    backgroundColor: COLORS.starbucksGreen,
    borderRadius: RADII.button,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 16,
  },
  confirmModalButtonText: { color: COLORS.white, fontSize: 16, fontWeight: '700' },
  closeModalButton: { paddingVertical: 10, alignItems: 'center', marginTop: 6 },
  closeModalButtonText: { color: COLORS.textBlackSoft, fontSize: 14, fontWeight: '600' },
});
