import React from 'react';
import { StyleSheet, View, Text, SafeAreaView, ScrollView } from 'react-native';
import { COLORS, TYPOGRAPHY, SPACING, SHADOWS, RADII } from '../styles/theme';
import Button from '../components/Button';
import { api } from '../services/api';

export default function HomeScreen({ user, subscription, onSignOut }) {
  const handleSignOut = async () => {
    await api.logout();
    onSignOut();
  };

  const initialLetter = user && user.name ? user.name[0].toUpperCase() : (user && user.email ? user.email[0].toUpperCase() : 'U');
  const userName = user && user.name ? user.name : (user && user.email ? user.email.split('@')[0] : 'User');

  const planTitle = subscription && subscription.planId ? subscription.planId.replace('PLAN_', '') : 'Active Member';
  const endsAtFormatted = subscription && subscription.endsAt ? new Date(subscription.endsAt).toLocaleDateString() : 'N/A';

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Top Navigation / Branding */}
        <View style={styles.navBar}>
          <Text style={styles.navLogo}>Svaadanusaar</Text>
          <View style={styles.avatarCircle}>
            <Text style={styles.avatarText}>{initialLetter}</Text>
          </View>
        </View>

        {/* Header Banner */}
        <View style={styles.featureBand}>
          <Text style={styles.welcomeTitle}>Welcome back, {userName}!</Text>
          <Text style={styles.welcomeSub}>Your personalized diet and health assistant is active and ready.</Text>
        </View>

        {/* Subscription Status Card */}
        <View style={styles.sectionContainer}>
          <Text style={styles.sectionTitle}>Active Membership</Text>

          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <Text style={styles.planName}>Subscription ({planTitle})</Text>
              <View style={styles.activeBadge}>
                <Text style={styles.activeBadgeText}>{subscription?.status || 'ACTIVE'}</Text>
              </View>
            </View>

            <Text style={styles.cardSub}>
              Valid until: <Text style={{ fontWeight: 'bold' }}>{endsAtFormatted}</Text>
            </Text>

            <View style={styles.divider} />

            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>AI Assistant Status</Text>
              <Text style={styles.infoValue}>Enabled</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Diet Plan Generator</Text>
              <Text style={styles.infoValue}>Unlocked</Text>
            </View>
          </View>
        </View>

        {/* Account Details Section */}
        <View style={styles.sectionContainer}>
          <Text style={styles.sectionTitle}>Account Details</Text>
          <View style={styles.card}>
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>Full Name</Text>
              <Text style={styles.detailValue}>{user?.name || 'N/A'}</Text>
            </View>
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>Email Address</Text>
              <Text style={styles.detailValue}>{user?.email || 'N/A'}</Text>
            </View>
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>Phone Number</Text>
              <Text style={styles.detailValue}>
                {user?.phone ? (() => {
                  const digits = user.phone.replace(/\D/g, '');
                  const ten = digits.length >= 10 ? digits.slice(-10) : digits;
                  return `+91-${ten.slice(0, 5)}-${ten.slice(5)}`;
                })() : 'N/A'}
              </Text>
            </View>
          </View>
        </View>

        {/* Sign Out Action */}
        <View style={styles.actionsContainer}>
          <Button
            title="Sign Out"
            type="outlined"
            onPress={handleSignOut}
            style={styles.signOutBtn}
          />
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
  },
  navBar: {
    height: 72,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: SPACING.outerGutter,
    backgroundColor: COLORS.white,
    ...SHADOWS.globalNav,
  },
  navLogo: {
    ...TYPOGRAPHY.titleSerif,
    fontSize: 24,
    color: COLORS.starbucksGreen,
  },
  avatarCircle: {
    width: 40,
    height: 40,
    borderRadius: RADII.circle,
    backgroundColor: COLORS.starbucksGreen,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    color: COLORS.white,
    fontWeight: 'bold',
    fontSize: 18,
  },
  featureBand: {
    backgroundColor: COLORS.houseGreen,
    padding: SPACING.space4,
    paddingVertical: SPACING.space5,
  },
  welcomeTitle: {
    ...TYPOGRAPHY.titleSerif,
    color: COLORS.white,
    fontSize: 24,
    marginBottom: SPACING.space1,
  },
  welcomeSub: {
    ...TYPOGRAPHY.body,
    color: COLORS.textWhiteSoft,
    fontSize: 15,
  },
  sectionContainer: {
    padding: SPACING.space4,
  },
  sectionTitle: {
    ...TYPOGRAPHY.h1,
    fontSize: 20,
    color: COLORS.textBlack,
    marginBottom: SPACING.space3,
  },
  card: {
    backgroundColor: COLORS.white,
    borderRadius: RADII.card,
    padding: SPACING.space4,
    ...SHADOWS.card,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.space1,
  },
  planName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: COLORS.textBlack,
  },
  activeBadge: {
    backgroundColor: '#e8f5e9',
    borderColor: COLORS.starbucksGreen,
    borderWidth: 1,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: RADII.button,
  },
  activeBadgeText: {
    ...TYPOGRAPHY.micro,
    color: COLORS.starbucksGreen,
    fontWeight: 'bold',
  },
  cardSub: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
  },
  divider: {
    height: 1,
    backgroundColor: COLORS.canvasCeramic,
    marginVertical: SPACING.space3,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: SPACING.space1,
  },
  infoLabel: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
  },
  infoValue: {
    ...TYPOGRAPHY.small,
    color: COLORS.starbucksGreen,
    fontWeight: 'bold',
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: SPACING.space2,
    borderBottomWidth: 1,
    borderBottomColor: COLORS.canvasCeramic,
  },
  detailLabel: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
  },
  detailValue: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlack,
    fontWeight: '600',
  },
  actionsContainer: {
    padding: SPACING.space4,
    marginBottom: SPACING.space6,
  },
  signOutBtn: {
    width: '100%',
  },
});
