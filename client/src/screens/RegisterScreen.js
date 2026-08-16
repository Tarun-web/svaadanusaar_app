import React, { useState } from 'react';
import { StyleSheet, View, Text, SafeAreaView, KeyboardAvoidingView, Platform, ScrollView, Pressable } from 'react-native';
import { COLORS, TYPOGRAPHY, SPACING } from '../styles/theme';
import FloatingLabelInput from '../components/FloatingLabelInput';
import Button from '../components/Button';
import { registerUser, setLoggedInUser } from '../services/mockDb';

export default function RegisterScreen({ phoneNumber, onBack, onRegisterSuccess }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [generalError, setGeneralError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Email regex validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const isEmailValid = emailRegex.test(email);
  const isPasswordValid = password.length >= 6;

  const handleEmailChange = (text) => {
    setEmail(text);
    if (emailError) setEmailError('');
    if (generalError) setGeneralError('');
  };

  const handlePasswordChange = (text) => {
    setPassword(text);
    if (passwordError) setPasswordError('');
    if (generalError) setGeneralError('');
  };

  const handleCreateAccount = async () => {
    let hasError = false;

    if (!isEmailValid) {
      setEmailError('Please enter a valid email address.');
      hasError = true;
    }

    if (!isPasswordValid) {
      setPasswordError('Password must be at least 6 characters long.');
      hasError = true;
    }

    if (hasError) return;

    setIsLoading(true);
    setGeneralError('');

    try {
      // Mock network delay
      setTimeout(async () => {
        try {
          const user = await registerUser(phoneNumber, email, password);
          await setLoggedInUser(user);
          setIsLoading(false);
          onRegisterSuccess(user);
        } catch (err) {
          setGeneralError(err.message || 'Registration failed.');
          setIsLoading(false);
        }
      }, 1000);
    } catch (err) {
      setGeneralError('Registration failed.');
      setIsLoading(false);
    }
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
              <Text style={styles.heading}>Create your profile</Text>
              <Text style={styles.subHeading}>
                We couldn't find an account for +1 ({phoneNumber.slice(0, 3)}) {phoneNumber.slice(3, 6)}-{phoneNumber.slice(6)}. Let's set up a new profile.
              </Text>

              {generalError ? <Text style={styles.generalError}>{generalError}</Text> : null}

              <FloatingLabelInput
                label="Email Address"
                value={email}
                onChangeText={handleEmailChange}
                keyboardType="email-address"
                autoCapitalize="none"
                error={emailError}
                isValid={isEmailValid}
              />

              <FloatingLabelInput
                label="Password (6+ characters)"
                value={password}
                onChangeText={handlePasswordChange}
                secureTextEntry
                autoCapitalize="none"
                error={passwordError}
                isValid={isPasswordValid}
              />
            </View>
          </View>

          <View style={styles.footer}>
            <Button
              title={isLoading ? 'Creating account...' : 'Create Account'}
              type="primary"
              disabled={!isEmailValid || !isPasswordValid || isLoading}
              onPress={handleCreateAccount}
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
  footer: {
    width: '100%',
  },
  btn: {
    width: '100%',
  },
});
