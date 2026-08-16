import React, { useState } from 'react';
import { StyleSheet, View, Text, SafeAreaView, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { COLORS, TYPOGRAPHY, SPACING } from '../styles/theme';
import FloatingLabelInput from '../components/FloatingLabelInput';
import Button from '../components/Button';

export default function PhoneInputScreen({ onNavigateToOtp }) {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [error, setError] = useState('');

  // Validate 10-digit number
  const cleanPhone = phoneNumber.replace(/\D/g, '');
  const isValid = cleanPhone.length === 10;

  const handlePhoneChange = (text) => {
    // Only allow numbers
    const cleanText = text.replace(/\D/g, '');
    
    // Format as (XXX) XXX-XXXX
    let formatted = cleanText;
    if (cleanText.length > 0) {
      if (cleanText.length <= 3) {
        formatted = `(${cleanText}`;
      } else if (cleanText.length <= 6) {
        formatted = `(${cleanText.slice(0, 3)}) ${cleanText.slice(3)}`;
      } else {
        formatted = `(${cleanText.slice(0, 3)}) ${cleanText.slice(3, 6)}-${cleanText.slice(6, 10)}`;
      }
    }
    
    setPhoneNumber(formatted);
    if (error) setError('');
  };

  const handleContinue = () => {
    if (!isValid) {
      setError('Please enter a valid 10-digit phone number.');
      return;
    }
    onNavigateToOtp(cleanPhone);
  };

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
          <View style={styles.header}>
            <Text style={styles.brandTitle}>Svaadanusaar</Text>
            <Text style={styles.brandSub}>REWARDS</Text>
          </View>

          <View style={styles.formContainer}>
            <Text style={styles.heading}>Enter your mobile number</Text>
            <Text style={styles.subHeading}>
              We will send you a text message with a verification code to check if you have an account with us.
            </Text>

            <FloatingLabelInput
              label="Mobile Number"
              value={phoneNumber}
              onChangeText={handlePhoneChange}
              keyboardType="phone-pad"
              maxLength={14} // (XXX) XXX-XXXX is 14 chars
              error={error}
              isValid={isValid}
            />

            <Text style={styles.legalText}>
              By continuing, you agree to receive a 6-digit verification code. Standard carrier text and data rates may apply. For demo purposes, the code is always <Text style={{ fontWeight: 'bold' }}>123456</Text>.
            </Text>
          </View>

          <View style={styles.footer}>
            <Button
              title="Continue"
              type="primary"
              disabled={!isValid}
              onPress={handleContinue}
              style={styles.btn}
            />
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.canvasWarm,
  },
  keyboardView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: SPACING.outerGutter,
    justifyContent: 'space-between',
    paddingBottom: SPACING.space6,
  },
  header: {
    marginTop: SPACING.space6,
    alignItems: 'center',
  },
  brandTitle: {
    ...TYPOGRAPHY.titleSerif,
    fontSize: 32,
    color: COLORS.starbucksGreen,
  },
  brandSub: {
    ...TYPOGRAPHY.micro,
    fontWeight: 'bold',
    letterSpacing: 2,
    color: COLORS.textBlackSoft,
    marginTop: -4,
  },
  formContainer: {
    flex: 1,
    justifyContent: 'center',
    marginVertical: SPACING.space6,
  },
  heading: {
    ...TYPOGRAPHY.h1,
    color: COLORS.textBlack,
    marginBottom: SPACING.space2,
  },
  subHeading: {
    ...TYPOGRAPHY.body,
    color: COLORS.textBlackSoft,
    marginBottom: SPACING.space4,
  },
  legalText: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
    marginTop: SPACING.space3,
    lineHeight: 18,
  },
  footer: {
    width: '100%',
    alignItems: 'flex-end',
  },
  btn: {
    width: '100%',
  },
});
