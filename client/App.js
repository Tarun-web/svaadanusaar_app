import React, { useState, useEffect } from 'react';
import { StyleSheet, View, ActivityIndicator, Text } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { COLORS } from './src/styles/theme';
import { api } from './src/services/api';

import PhoneInputScreen from './src/screens/PhoneInputScreen';
import OtpScreen from './src/screens/OtpScreen';
import RegisterScreen from './src/screens/RegisterScreen';
import SubscriptionPlansScreen from './src/screens/SubscriptionPlansScreen';
import HomeScreen from './src/screens/HomeScreen';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState('PhoneInput');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [user, setUser] = useState(null);
  const [subscription, setSubscription] = useState(null);
  const [isInitializing, setIsInitializing] = useState(true);

  // Check existing session on app startup
  useEffect(() => {
    async function checkAuthSession() {
      try {
        const token = api.getAuthToken();
        if (!token) {
          setCurrentScreen('PhoneInput');
          setIsInitializing(false);
          return;
        }

        const profile = await api.getUserProfile();
        if (profile) {
          setUser(profile);
          const subStatus = await api.getSubscriptionStatus();
          setSubscription(subStatus);

          if (!profile.name || !profile.email) {
            setCurrentScreen('Register');
          } else if (!subStatus || subStatus.status !== 'ACTIVE') {
            setCurrentScreen('SubscriptionPlans');
          } else {
            setCurrentScreen('Home');
          }
        } else {
          setCurrentScreen('PhoneInput');
        }
      } catch (error) {
        console.log('No active session found, prompting phone input login.');
        setCurrentScreen('PhoneInput');
      } finally {
        setIsInitializing(false);
      }
    }
    checkAuthSession();
  }, []);

  const handleNavigateToOtp = (phone) => {
    setPhoneNumber(phone);
    setCurrentScreen('OtpVerify');
  };

  // Called after OTP is verified successfully
  const handleVerificationSuccess = ({ profile, subscription: subStatus }) => {
    setUser(profile);
    setSubscription(subStatus);

    if (!profile.name || !profile.email) {
      setCurrentScreen('Register');
    } else if (!subStatus || subStatus.status !== 'ACTIVE') {
      setCurrentScreen('SubscriptionPlans');
    } else {
      setCurrentScreen('Home');
    }
  };

  // Called after profile setup (Name & Email) is saved
  const handleRegisterSuccess = (updatedProfile) => {
    setUser(updatedProfile);

    // If no active subscription, force routing to Subscription Plans screen
    if (!subscription || subscription.status !== 'ACTIVE') {
      setCurrentScreen('SubscriptionPlans');
    } else {
      setCurrentScreen('Home');
    }
  };

  // Called after plan subscription is successful
  const handleSubscriptionSuccess = (subResult) => {
    setSubscription(subResult);
    setCurrentScreen('Home');
  };

  // Sign out handler
  const handleSignOut = async () => {
    await api.logout();
    setUser(null);
    setSubscription(null);
    setPhoneNumber('');
    setCurrentScreen('PhoneInput');
  };

  if (isInitializing) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={COLORS.accentGreen} />
        <Text style={styles.loadingText}>Loading Svaadanusaar...</Text>
      </View>
    );
  }

  // Security guard: Ensure Home screen cannot be accessed without active subscription
  if (currentScreen === 'Home' && (!subscription || subscription.status !== 'ACTIVE')) {
    setCurrentScreen('SubscriptionPlans');
  }

  return (
    <View style={styles.container}>
      <StatusBar style="auto" />
      {currentScreen === 'PhoneInput' && (
        <PhoneInputScreen onNavigateToOtp={handleNavigateToOtp} />
      )}
      {currentScreen === 'OtpVerify' && (
        <OtpScreen
          phoneNumber={phoneNumber}
          onBack={() => setCurrentScreen('PhoneInput')}
          onVerificationSuccess={handleVerificationSuccess}
        />
      )}
      {currentScreen === 'Register' && (
        <RegisterScreen
          phoneNumber={phoneNumber}
          onBack={() => setCurrentScreen('OtpVerify')}
          onRegisterSuccess={handleRegisterSuccess}
        />
      )}
      {currentScreen === 'SubscriptionPlans' && (
        <SubscriptionPlansScreen
          onSubscriptionSuccess={handleSubscriptionSuccess}
          onSignOut={handleSignOut}
        />
      )}
      {currentScreen === 'Home' && (
        <HomeScreen
          user={user}
          subscription={subscription}
          onSignOut={handleSignOut}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.canvasWarm,
  },
  loadingContainer: {
    flex: 1,
    backgroundColor: COLORS.canvasWarm,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    color: COLORS.starbucksGreen,
    fontWeight: '600',
    fontSize: 16,
  },
});
