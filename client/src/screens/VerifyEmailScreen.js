import React, { useState } from 'react';
import { StyleSheet, View, Text, SafeAreaView, ScrollView, ActivityIndicator } from 'react-native';
import { COLORS, TYPOGRAPHY, SPACING, RADII, SHADOWS } from '../styles/theme';
import Button from '../components/Button';
import { api } from '../services/api';

export default function VerifyEmailScreen({ user, onVerificationConfirmed, onSignOut }) {
  const [isChecking, setIsChecking] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');
  const [isError, setIsError] = useState(false);

  const email = user?.email || 'your email';

  const handleCheckStatus = async () => {
    setIsChecking(true);
    setStatusMessage('');
    setIsError(false);

    try {
      // Fetch latest user profile from database
      const updatedProfile = await api.getUserProfile();

      if (updatedProfile && updatedProfile.emailVerified === true) {
        setStatusMessage('Email verified successfully!');
        setIsChecking(false);
        setTimeout(() => {
          onVerificationConfirmed(updatedProfile);
        }, 800);
      } else {
        setIsError(true);
        setStatusMessage('Email is not verified in our database yet. Please check your inbox and click the verification link first.');
        setIsChecking(false);
      }
    } catch (err) {
      console.log('Verification check error:', err);
      setIsError(true);
      setStatusMessage('Unable to check verification status right now. Please try again.');
      setIsChecking(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.header}>
          <Text style={styles.brandTitle}>Svaadanusaar</Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.icon}>✉️</Text>
          <Text style={styles.heading}>Verify Your Email</Text>
          <Text style={styles.subHeading}>
            A verification link has been sent to{' '}
            <Text style={styles.emailText}>{email}</Text>.
          </Text>
          <Text style={styles.infoText}>
            Please check your inbox and click the link to verify your account. You must verify your email before accessing subscription plans or your dashboard.
          </Text>

          {statusMessage ? (
            <View style={[styles.statusBox, isError ? styles.statusBoxError : styles.statusBoxSuccess]}>
              <Text style={[styles.statusText, isError ? styles.statusTextError : styles.statusTextSuccess]}>
                {statusMessage}
              </Text>
            </View>
          ) : null}

          {isChecking ? (
            <View style={styles.loadingBox}>
              <ActivityIndicator size="small" color={COLORS.starbucksGreen} />
              <Text style={styles.loadingText}>Verifying email status...</Text>
            </View>
          ) : (
            <View style={styles.buttonGroup}>
              <Button
                title="I've Verified My Email"
                type="primary"
                onPress={handleCheckStatus}
                style={styles.btn}
              />
              <Button
                title="Sign Out"
                type="secondary"
                onPress={onSignOut}
                style={styles.btnSecondary}
              />
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.canvasWarm,
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: SPACING.outerGutter,
    justifyContent: 'center',
    paddingVertical: SPACING.space6,
  },
  header: {
    alignItems: 'center',
    marginBottom: SPACING.space6,
  },
  brandTitle: {
    ...TYPOGRAPHY.titleSerif,
    fontSize: 32,
    color: COLORS.starbucksGreen,
  },
  card: {
    backgroundColor: COLORS.white,
    borderRadius: RADII.card,
    padding: SPACING.space6,
    alignItems: 'center',
    ...SHADOWS.card,
  },
  icon: {
    fontSize: 48,
    marginBottom: SPACING.space3,
  },
  heading: {
    ...TYPOGRAPHY.h1,
    color: COLORS.textBlack,
    textAlign: 'center',
    marginBottom: SPACING.space2,
  },
  subHeading: {
    ...TYPOGRAPHY.body,
    color: COLORS.textBlackSoft,
    textAlign: 'center',
    marginBottom: SPACING.space3,
  },
  emailText: {
    fontWeight: 'bold',
    color: COLORS.accentGreen,
  },
  infoText: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: SPACING.space5,
  },
  statusBox: {
    width: '100%',
    padding: SPACING.space3,
    borderRadius: RADII.button,
    marginBottom: SPACING.space4,
  },
  statusBoxError: {
    backgroundColor: COLORS.errorBgTint,
    borderWidth: 1,
    borderColor: COLORS.errorRed,
  },
  statusBoxSuccess: {
    backgroundColor: COLORS.validBgTint,
    borderWidth: 1,
    borderColor: COLORS.accentGreen,
  },
  statusText: {
    ...TYPOGRAPHY.small,
    textAlign: 'center',
    fontWeight: '600',
  },
  statusTextError: {
    color: COLORS.errorRed,
  },
  statusTextSuccess: {
    color: COLORS.starbucksGreen,
  },
  loadingBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: SPACING.space3,
  },
  loadingText: {
    ...TYPOGRAPHY.small,
    color: COLORS.starbucksGreen,
    fontWeight: '600',
  },
  buttonGroup: {
    width: '100%',
    gap: 12,
  },
  btn: {
    width: '100%',
  },
  btnSecondary: {
    width: '100%',
    marginTop: 4,
  },
});
