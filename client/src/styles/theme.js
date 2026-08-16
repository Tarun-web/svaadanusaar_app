export const COLORS = {
  // Primary Greens
  starbucksGreen: '#006241', // Historic brand green (headings)
  accentGreen: '#00754A',    // Brighter green (primary CTAs)
  houseGreen: '#1E3932',     // Near-black deep green (feature bands/footers)
  upliftGreen: '#2b5148',    // Mid-dark decorative accent
  lightGreen: '#d4e9e2',     // Pale mint wash (valid states/bg washes)

  // Gold (Rewards exclusive)
  gold: '#cba258',
  goldLight: '#dfc49d',
  goldLightest: '#faf6ee',

  // Surfaces & Backgrounds
  white: '#ffffff',
  neutralCool: '#f9f9f9',    // Forms/Dropdown wraps
  canvasWarm: '#f2f0eb',     // Cream primary page canvas
  canvasCeramic: '#edebe9',  // Soft zone separators
  black: '#000000',

  // Text
  textBlack: 'rgba(0, 0, 0, 0.87)',      // Primary text on light surface
  textBlackSoft: 'rgba(0, 0, 0, 0.58)',  // Secondary text on light surface
  textWhite: '#ffffff',                  // Primary text on dark surface
  textWhiteSoft: 'rgba(255, 255, 255, 0.70)', // Secondary text on dark surface
  rewardsGreen: '#33433d',               // Muted Rewards text

  // Semantic
  errorRed: '#c82014',
  errorBgTint: 'rgba(200, 32, 20, 0.05)',  // Red Tint
  validBgTint: 'rgba(212, 233, 226, 0.33)', // Green Light at 33%
};

export const SHADOWS = {
  card: {
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.20,
    shadowRadius: 1.5,
    elevation: 2,
  },
  globalNav: {
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 4,
  },
  frap: {
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.14,
    shadowRadius: 12,
    elevation: 6,
  },
};

export const TYPOGRAPHY = {
  display: {
    fontSize: 36,
    fontWeight: '600',
    letterSpacing: -0.16,
    color: COLORS.textBlack,
  },
  titleSerif: {
    fontFamily: 'Georgia',
    fontSize: 24,
    fontWeight: 'bold',
    letterSpacing: -0.16,
    color: COLORS.houseGreen,
  },
  h1: {
    fontSize: 24,
    fontWeight: '600',
    letterSpacing: -0.16,
    color: COLORS.starbucksGreen,
  },
  h2: {
    fontSize: 24,
    fontWeight: '400',
    letterSpacing: -0.16,
    color: COLORS.textBlack,
  },
  bodyLarge: {
    fontSize: 18,
    lineHeight: 28,
    letterSpacing: -0.16,
    color: COLORS.textBlack,
  },
  body: {
    fontSize: 16,
    lineHeight: 24,
    letterSpacing: -0.1,
    color: COLORS.textBlack,
  },
  small: {
    fontSize: 14,
    lineHeight: 20,
    letterSpacing: -0.1,
    color: COLORS.textBlackSoft,
  },
  micro: {
    fontSize: 12,
    lineHeight: 18,
    letterSpacing: -0.1,
    color: COLORS.textBlackSoft,
  },
};

export const SPACING = {
  space1: 4,
  space2: 8,
  space3: 16,
  space4: 24,
  space5: 32,
  space6: 40,
  space7: 48,
  space8: 56,
  space9: 64,
  outerGutter: 16,
};

export const RADII = {
  card: 12,
  button: 50,
  circle: 9999,
};

export const ANIMATION = {
  buttonActiveScale: 0.95,
  expanderDuration: 300,
};
