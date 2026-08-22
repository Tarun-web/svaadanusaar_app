import React, { useState, useEffect, useRef } from 'react';
import { StyleSheet, View, TextInput, Animated, Text } from 'react-native';
import { COLORS, TYPOGRAPHY, RADII } from '../styles/theme';

export default function FloatingLabelInput({
  label,
  value = '',
  onChangeText,
  error = false,
  isValid = false,
  keyboardType = 'default',
  secureTextEntry = false,
  containerStyle,
  ...props
}) {
  const [isFocused, setIsFocused] = useState(false);
  const animatedIsFocused = useRef(new Animated.Value(value ? 1 : 0)).current;

  useEffect(() => {
    Animated.timing(animatedIsFocused, {
      toValue: (isFocused || value) ? 1 : 0,
      duration: 200,
      useNativeDriver: false,
    }).start();
  }, [isFocused, value]);

  const labelStyle = {
    position: 'absolute',
    left: 20,
    top: animatedIsFocused.interpolate({
      inputRange: [0, 1],
      outputRange: [18, 6],
    }),
    fontSize: animatedIsFocused.interpolate({
      inputRange: [0, 1],
      outputRange: [16, 11],
    }),
    color: animatedIsFocused.interpolate({
      inputRange: [0, 1],
      outputRange: [COLORS.textBlackSoft, error ? COLORS.errorRed : COLORS.accentGreen],
    }),
    fontWeight: '500',
  };

  const getContainerBg = () => {
    if (error) return COLORS.errorBgTint;
    if (isValid) return COLORS.validBgTint;
    return COLORS.white;
  };

  const getBorderColor = () => {
    if (error) return COLORS.errorRed;
    if (isFocused) return COLORS.accentGreen;
    return COLORS.canvasCeramic;
  };

  return (
    <View style={styles.outerContainer}>
      <View
        style={[
          styles.container,
          {
            backgroundColor: getContainerBg(),
            borderColor: getBorderColor(),
          },
          containerStyle,
        ]}
      >
        <Animated.Text style={labelStyle}>{label}</Animated.Text>
        <TextInput
          {...props}
          style={styles.textInput}
          value={value}
          onChangeText={onChangeText}
          keyboardType={keyboardType}
          secureTextEntry={secureTextEntry}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          blurOnSubmit
          cursorColor={COLORS.accentGreen}
          selectionColor={COLORS.accentGreen}
        />
      </View>
      {error && typeof error === 'string' ? (
        <Text style={styles.errorText}>{error}</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  outerContainer: {
    marginVertical: 8,
    width: '100%',
  },
  container: {
    flexDirection: 'row',
    height: 56,
    borderWidth: 1,
    borderRadius: RADII.button, // Full-pill rounded shape (50px)
    paddingTop: 12,
    position: 'relative',
    transition: 'background-color 0.2s ease, border-color 0.2s ease',
  },
  textInput: {
    flex: 1,
    paddingHorizontal: 20,
    color: COLORS.textBlack,
    fontSize: 16,
    textAlignVertical: 'center',
  },
  errorText: {
    ...TYPOGRAPHY.micro,
    color: COLORS.errorRed,
    marginTop: 4,
    marginLeft: 12,
    fontWeight: '500',
  },
});
