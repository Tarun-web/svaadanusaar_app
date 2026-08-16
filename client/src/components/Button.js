import React, { useRef } from 'react';
import { StyleSheet, Text, Pressable, Animated } from 'react-native';
import { COLORS, RADII, TYPOGRAPHY, SPACING } from '../styles/theme';

export default function Button({
  title,
  onPress,
  type = 'primary', // 'primary' | 'outlined' | 'black' | 'outlinedDark'
  disabled = false,
  style,
  textStyle,
  children,
}) {
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const handlePressIn = () => {
    Animated.spring(scaleAnim, {
      toValue: 0.95,
      useNativeDriver: true,
      speed: 50,
      bounciness: 0,
    }).start();
  };

  const handlePressOut = () => {
    Animated.spring(scaleAnim, {
      toValue: 1,
      useNativeDriver: true,
      speed: 50,
      bounciness: 0,
    }).start();
  };

  const getButtonStyles = () => {
    switch (type) {
      case 'outlined':
        return [styles.button, styles.outlined, disabled && styles.disabledOutlined];
      case 'black':
        return [styles.button, styles.black, disabled && styles.disabled];
      case 'outlinedDark':
        return [styles.button, styles.outlinedDark, disabled && styles.disabledOutlinedDark];
      case 'primary':
      default:
        return [styles.button, styles.primary, disabled && styles.disabled];
    }
  };

  const getTextStyles = () => {
    switch (type) {
      case 'outlined':
        return [styles.text, styles.textOutlined, disabled && styles.textDisabledOutlined];
      case 'outlinedDark':
        return [styles.text, styles.textOutlinedDark, disabled && styles.textDisabledOutlinedDark];
      case 'black':
      case 'primary':
      default:
        return [styles.text, styles.textPrimary, disabled && styles.textDisabled];
    }
  };

  return (
    <Animated.View style={{ transform: [{ scale: scaleAnim }], width: style?.width }}>
      <Pressable
        onPress={onPress}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        disabled={disabled}
        style={({ pressed }) => [
          getButtonStyles(),
          style,
        ]}
      >
        {children ? (
          children
        ) : (
          <Text style={[getTextStyles(), textStyle]}>{title}</Text>
        )}
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  button: {
    borderRadius: RADII.button,
    paddingVertical: 12,
    paddingHorizontal: 24,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'transparent',
    minHeight: 48,
  },
  primary: {
    backgroundColor: COLORS.accentGreen,
    borderColor: COLORS.accentGreen,
  },
  outlined: {
    backgroundColor: 'transparent',
    borderColor: COLORS.accentGreen,
  },
  outlinedDark: {
    backgroundColor: 'transparent',
    borderColor: COLORS.textWhite,
  },
  black: {
    backgroundColor: COLORS.black,
    borderColor: COLORS.black,
  },
  disabled: {
    backgroundColor: 'rgba(0, 0, 0, 0.12)',
    borderColor: 'transparent',
  },
  disabledOutlined: {
    borderColor: 'rgba(0, 0, 0, 0.12)',
  },
  disabledOutlinedDark: {
    borderColor: 'rgba(255, 255, 255, 0.3)',
  },
  text: {
    ...TYPOGRAPHY.small,
    fontWeight: '600',
    textAlign: 'center',
  },
  textPrimary: {
    color: COLORS.white,
  },
  textOutlined: {
    color: COLORS.accentGreen,
  },
  textOutlinedDark: {
    color: COLORS.textWhite,
  },
  textDisabled: {
    color: 'rgba(0, 0, 0, 0.38)',
  },
  textDisabledOutlined: {
    color: 'rgba(0, 0, 0, 0.38)',
  },
  textDisabledOutlinedDark: {
    color: 'rgba(255, 255, 255, 0.3)',
  },
});
