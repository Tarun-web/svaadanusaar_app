import React, { useState, useRef, useEffect } from 'react';
import { StyleSheet, View, Text, SafeAreaView, KeyboardAvoidingView, Platform, ScrollView, TextInput, Pressable } from 'react-native';
import { COLORS, TYPOGRAPHY, SPACING, RADII } from '../styles/theme';
import Button from '../components/Button';
import { findUserByPhone, setLoggedInUser } from '../services/mockDb';

export default function OtpScreen({ phoneNumber, onBack, onNavigateToRegister, onLoginSuccess }) {
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const inputRefs = useRef([]);

  // Auto-focus first input on mount
  useEffect(() => {
    if (inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
  }, []);

  const handleChangeText = (text, index) => {
    const numericText = text.replace(/[^0-9]/g, '');
    const newOtp = [...otp];
    newOtp[index] = numericText;
    setOtp(newOtp);
    setError('');

    // If typing a digit and not on the last input, focus next input
    if (numericText.length === 1 && index < 5) {
      inputRefs.current[index + 1].focus();
    }
  };

  const handleKeyPress = (e, index) => {
    // If pressing backspace and the current value is empty, focus previous input
    if (e.nativeEvent.key === 'Backspace' && otp[index] === '' && index > 0) {
      const newOtp = [...otp];
      newOtp[index - 1] = ''; // clear previous
      setOtp(newOtp);
      inputRefs.current[index - 1].focus();
    }
  };

  const otpCode = otp.join('');
  const isComplete = otpCode.length === 6;

  const handleVerify = async () => {
    if (!isComplete) return;

    setIsLoading(true);
    setError('');

    // Simulate network delay
    setTimeout(async () => {
      if (otpCode !== '123456') {
        setError('Incorrect verification code. Please try again.');
        setOtp(['', '', '', '', '', '']);
        inputRefs.current[0].focus();
        setIsLoading(false);
        return;
      }

      try {
        const user = await findUserByPhone(phoneNumber);
        setIsLoading(false);
        if (user) {
          await setLoggedInUser(user);
          onLoginSuccess(user);
        } else {
          onNavigateToRegister(phoneNumber);
        }
      } catch (err) {
        setError('An error occurred during verification.');
        setIsLoading(false);
      }
    }, 1000);
  };

  const formatDisplayPhone = (rawPhone) => {
    if (rawPhone.length === 10) {
      return `+1 (${rawPhone.slice(0, 3)}) ${rawPhone.slice(3, 6)}-${rawPhone.slice(6)}`;
    }
    return rawPhone;
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
              <Text style={styles.heading}>Verify phone number</Text>
              <Text style={styles.subHeading}>
                Enter the 6-digit verification code sent to{' '}
                <Text style={{ fontWeight: 'bold' }}>{formatDisplayPhone(phoneNumber)}</Text>
              </Text>

              <View style={styles.otpGrid}>
                {otp.map((digit, index) => (
                  <TextInput
                    key={index}
                    ref={(ref) => (inputRefs.current[index] = ref)}
                    style={[
                      styles.otpInput,
                      digit ? styles.otpInputFilled : null,
                      error ? styles.otpInputError : null,
                    ]}
                    value={digit}
                    onChangeText={(text) => handleChangeText(text, index)}
                    onKeyPress={(e) => handleKeyPress(e, index)}
                    keyboardType="numeric"
                    maxLength={1}
                    selectTextOnFocus
                  />
                ))}
              </View>

              {error ? <Text style={styles.errorText}>{error}</Text> : null}

              <Text style={styles.resendText}>
                Didn't get code? <Text style={styles.resendLink}>Resend code (123456)</Text>
              </Text>
            </View>
          </View>

          <View style={styles.footer}>
            <Button
              title={isLoading ? 'Verifying...' : 'Verify & Continue'}
              type="primary"
              disabled={!isComplete || isLoading}
              onPress={handleVerify}
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
  otpGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: SPACING.space4,
  },
  otpInput: {
    width: 46,
    height: 56,
    borderWidth: 1,
    borderColor: COLORS.canvasCeramic,
    borderRadius: 8,
    backgroundColor: COLORS.white,
    textAlign: 'center',
    fontSize: 20,
    fontWeight: 'bold',
    color: COLORS.textBlack,
  },
  otpInputFilled: {
    borderColor: COLORS.accentGreen,
    backgroundColor: COLORS.validBgTint,
  },
  otpInputError: {
    borderColor: COLORS.errorRed,
    backgroundColor: COLORS.errorBgTint,
  },
  errorText: {
    ...TYPOGRAPHY.micro,
    color: COLORS.errorRed,
    fontWeight: '500',
    marginBottom: SPACING.space3,
  },
  resendText: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
    marginTop: SPACING.space2,
  },
  resendLink: {
    color: COLORS.accentGreen,
    fontWeight: '600',
  },
  footer: {
    width: '100%',
  },
  btn: {
    width: '100%',
  },
});
