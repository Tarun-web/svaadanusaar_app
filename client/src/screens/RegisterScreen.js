import React, { useState } from 'react';
import { StyleSheet, View, Text, SafeAreaView, KeyboardAvoidingView, Platform, ScrollView, Pressable } from 'react-native';
import { COLORS, TYPOGRAPHY, SPACING } from '../styles/theme';
import FloatingLabelInput from '../components/FloatingLabelInput';
import Button from '../components/Button';
import { api } from '../services/api';

export default function RegisterScreen({ phoneNumber, onBack, onRegisterSuccess }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [nameError, setNameError] = useState('');
  const [emailError, setEmailError] = useState('');
  const [generalError, setGeneralError] = useState('');
  const [emailSentNotice, setEmailSentNotice] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // Email regex validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const isEmailValid = emailRegex.test(email.trim());
  const isNameValid = name.trim().length >= 2;

  const handleNameChange = (text) => {
    setName(text);
    if (nameError) setNameError('');
    if (generalError) setGeneralError('');
  };

  const handleEmailChange = (text) => {
    setEmail(text);
    if (emailError) setEmailError('');
    if (generalError) setGeneralError('');
  };

  const handleSaveProfile = async () => {
    let hasError = false;

    if (!isNameValid) {
      setNameError('Please enter your full name (at least 2 characters).');
      hasError = true;
    }

    if (!isEmailValid) {
      setEmailError('Please enter a valid email address.');
      hasError = true;
    }

    if (hasError) return;

    setIsLoading(true);
    setGeneralError('');

    try {
      // Call PUT /api/v1/users/me -> updates backend name/email and triggers verification email
      const updatedUser = await api.updateUserProfile(name.trim(), email.trim());
      setEmailSentNotice(true);
      setIsLoading(false);

      // Short pause to allow user to see notice before transitioning to subscription plans
      setTimeout(() => {
        onRegisterSuccess(updatedUser);
      }, 1500);
    } catch (err) {
      console.error('Update Profile Error:', err);
      setGeneralError(err.message || 'Failed to update profile.');
      setIsLoading(false);
    }
  };

  const formatDisplayPhone = (rawPhone) => {
    if (!rawPhone) return '';
    const digits = rawPhone.replace(/\D/g, '');
    const ten = digits.length >= 10 ? digits.slice(-10) : digits;
    return `+91-${ten.slice(0, 5)}-${ten.slice(5)}`;
  };

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
          <View>
            <Pressable onPress={onBack} style={styles.backButton}>
              <Text style={styles.backButtonText}>← Back</Text>
            </Pressable>

            <View style={styles.contentContainer}>
              <Text style={styles.heading}>Complete Your Profile</Text>
              <Text style={styles.subHeading}>
                Welcome to Svaadanusaar! Please provide your name and email to set up your account for{' '}
                <Text style={{ fontWeight: 'bold' }}>{formatDisplayPhone(phoneNumber)}</Text>.
              </Text>

              {generalError ? <Text style={styles.generalError}>{generalError}</Text> : null}
              
              {emailSentNotice ? (
                <View style={styles.successBanner}>
                  <Text style={styles.successBannerTitle}>✓ Profile Updated & Verification Sent</Text>
                  <Text style={styles.successBannerText}>
                    A verification link has been sent to {email}. Please check your inbox and click the link to verify your email.
                  </Text>
                </View>
              ) : null}

              <FloatingLabelInput
                label="Full Name"
                value={name}
                onChangeText={handleNameChange}
                autoCapitalize="words"
                error={nameError}
                isValid={isNameValid}
              />

              <FloatingLabelInput
                label="Email Address"
                value={email}
                onChangeText={handleEmailChange}
                keyboardType="email-address"
                autoCapitalize="none"
                error={emailError}
                isValid={isEmailValid}
              />
            </View>
          </View>

          <View style={styles.footer}>
            <Button
              title={isLoading ? 'Saving...' : 'Save & Continue'}
              type="primary"
              disabled={!isNameValid || !isEmailValid || isLoading}
              onPress={handleSaveProfile}
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
  backButton: {
    marginTop: SPACING.space3,
    paddingVertical: SPACING.space2,
  },
  backButtonText: {
    ...TYPOGRAPHY.small,
    color: COLORS.accentGreen,
    fontWeight: '700',
  },
  contentContainer: {
    marginTop: SPACING.space4,
  },
  heading: {
    ...TYPOGRAPHY.h1,
    color: COLORS.textBlack,
    marginBottom: SPACING.space2,
  },
  subHeading: {
    ...TYPOGRAPHY.body,
    color: COLORS.textBlackSoft,
    marginBottom: SPACING.space5,
  },
  generalError: {
    ...TYPOGRAPHY.small,
    color: COLORS.errorRed,
    backgroundColor: COLORS.errorBgTint,
    padding: SPACING.space3,
    borderRadius: 8,
    marginBottom: SPACING.space3,
    fontWeight: '600',
  },
  successBanner: {
    backgroundColor: '#e8f5e9',
    borderWidth: 1,
    borderColor: COLORS.accentGreen,
    padding: SPACING.space3,
    borderRadius: 8,
    marginBottom: SPACING.space4,
  },
  successBannerTitle: {
    ...TYPOGRAPHY.small,
    color: COLORS.starbucksGreen,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  successBannerText: {
    ...TYPOGRAPHY.micro,
    color: COLORS.textBlack,
    lineHeight: 18,
  },
  footer: {
    width: '100%',
  },
  btn: {
    width: '100%',
  },
});
