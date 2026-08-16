import React, { useState, useEffect } from 'react';
import { StyleSheet, View, ActivityIndicator, Text } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { COLORS } from './src/styles/theme';
import { getLoggedInUser } from './src/services/mockDb';

import PhoneInputScreen from './src/screens/PhoneInputScreen';
import OtpScreen from './src/screens/OtpScreen';
import RegisterScreen from './src/screens/RegisterScreen';
import HomeScreen from './src/screens/HomeScreen';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState('PhoneInput');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [loggedInUser, setLoggedInUser] = useState(null);
  const [isInitializing, setIsInitializing] = useState(true);

  // Check if user is already logged in on app startup
  useEffect(() => {
    async function checkAuthSession() {
      try {
        const user = await getLoggedInUser();
        if (user) {
          setLoggedInUser(user);
          setCurrentScreen('Home');
        }
      } catch (error) {
        console.error('Error reading auth session:', error);
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

  const handleNavigateToRegister = (phone) => {
    setPhoneNumber(phone);
    setCurrentScreen('Register');
  };

  const handleLoginSuccess = (user) => {
    setLoggedInUser(user);
    setCurrentScreen('Home');
  };

  const handleRegisterSuccess = (user) => {
    setLoggedInUser(user);
    setCurrentScreen('Home');
  };

  const handleSignOut = () => {
    setLoggedInUser(null);
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
          onNavigateToRegister={handleNavigateToRegister}
          onLoginSuccess={handleLoginSuccess}
        />
      )}
      {currentScreen === 'Register' && (
        <RegisterScreen
          phoneNumber={phoneNumber}
          onBack={() => setCurrentScreen('OtpVerify')}
          onRegisterSuccess={handleRegisterSuccess}
        />
      )}
      {currentScreen === 'Home' && (
        <HomeScreen user={loggedInUser} onSignOut={handleSignOut} />
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
