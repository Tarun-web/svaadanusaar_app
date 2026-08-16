import React from 'react';
import { StyleSheet, View, Text, SafeAreaView, ScrollView, Pressable } from 'react-native';
import { COLORS, TYPOGRAPHY, SPACING, SHADOWS, RADII } from '../styles/theme';
import Button from '../components/Button';
import { clearDatabase, setLoggedInUser } from '../services/mockDb';

export default function HomeScreen({ user, onSignOut }) {
  const handleSignOut = async () => {
    await setLoggedInUser(null);
    onSignOut();
  };

  const handleResetDb = async () => {
    await clearDatabase();
    onSignOut();
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Top Header/Brand Bar */}
        <View style={styles.navBar}>
          <Text style={styles.navLogo}>Svaadanusaar</Text>
          <View style={styles.avatarCircle}>
            <Text style={styles.avatarText}>{user.email[0].toUpperCase()}</Text>
          </View>
        </View>

        {/* Feature Band - Starbucks dark green House Green style */}
        <View style={styles.featureBand}>
          <Text style={styles.welcomeTitle}>Svaadanusaar Rewards</Text>
          <Text style={styles.welcomeSub}>Hello, {user.email.split('@')[0]}! Enjoy your delicious journey.</Text>

          {/* Star Balance Display */}
          <View style={styles.starRow}>
            <Text style={styles.starCount}>250</Text>
            <Text style={styles.starSymbol}>★</Text>
            <View style={styles.goldBadge}>
              <Text style={styles.goldBadgeText}>GOLD LEVEL</Text>
            </View>
          </View>

          <Text style={styles.starSub}>You are 150★ away from a free handcrafted beverage!</Text>
        </View>

        {/* Rewards Tiers Section - Starbucks-inspired card system */}
        <View style={styles.rewardsSection}>
          <Text style={styles.sectionTitle}>Your Rewards Tiers</Text>

          <View style={styles.rewardCard}>
            <View style={styles.rewardHeader}>
              <Text style={styles.rewardStars}>25 ★</Text>
              <Text style={styles.rewardBadge}>Tier 1</Text>
            </View>
            <Text style={styles.rewardDesc}>Customize your drink: Extra espresso shot, dairy substitute, or pump of syrup.</Text>
          </View>

          <View style={styles.rewardCard}>
            <View style={styles.rewardHeader}>
              <Text style={styles.rewardStars}>100 ★</Text>
              <Text style={styles.rewardBadge}>Tier 2</Text>
            </View>
            <Text style={styles.rewardDesc}>Brewed hot/iced coffee, bakery items like croissants or chocolate cookies.</Text>
          </View>

          <View style={styles.rewardCard}>
            <View style={styles.rewardHeader}>
              <Text style={[styles.rewardStars, { color: COLORS.gold }]}>200 ★</Text>
              <View style={[styles.goldBadge, { marginHorizontal: 0 }]}>
                <Text style={styles.goldBadgeText}>Popular</Text>
              </View>
            </View>
            <Text style={styles.rewardDesc}>Handcrafted hot or cold beverage (Lattes, Frappuccinos) or hot breakfast sandwich.</Text>
          </View>
        </View>

        {/* User Profile / Dev Details */}
        <View style={styles.profileSection}>
          <Text style={styles.sectionTitle}>Account Details</Text>
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Email</Text>
            <Text style={styles.detailValue}>{user.email}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Phone Number</Text>
            <Text style={styles.detailValue}>
              +1 ({user.phone.slice(0, 3)}) {user.phone.slice(3, 6)}-{user.phone.slice(6)}
            </Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Member Since</Text>
            <Text style={styles.detailValue}>
              {new Date(user.createdAt).toLocaleDateString()}
            </Text>
          </View>
        </View>

        {/* Actions / Sign Out */}
        <View style={styles.actionsContainer}>
          <Button
            title="Sign Out"
            type="outlined"
            onPress={handleSignOut}
            style={styles.signOutBtn}
          />
          <Button
            title="Clear Mock DB & Restart"
            type="black"
            onPress={handleResetDb}
            style={styles.resetBtn}
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
    fontSize: 22,
    color: COLORS.starbucksGreen,
  },
  avatarCircle: {
    width: 40,
    height: 40,
    borderRadius: RADII.circle,
    backgroundColor: COLORS.accentGreen,
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
    marginBottom: SPACING.space4,
  },
  starRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
    marginBottom: SPACING.space1,
  },
  starCount: {
    fontSize: 48,
    fontWeight: 'bold',
    color: COLORS.white,
  },
  starSymbol: {
    fontSize: 32,
    color: COLORS.gold,
    marginLeft: 4,
    marginRight: SPACING.space3,
  },
  goldBadge: {
    backgroundColor: COLORS.gold,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: RADII.button,
    marginHorizontal: SPACING.space2,
  },
  goldBadgeText: {
    ...TYPOGRAPHY.micro,
    color: COLORS.houseGreen,
    fontWeight: 'bold',
  },
  starSub: {
    ...TYPOGRAPHY.small,
    color: COLORS.textWhiteSoft,
  },
  rewardsSection: {
    padding: SPACING.space4,
  },
  sectionTitle: {
    ...TYPOGRAPHY.h1,
    color: COLORS.textBlack,
    marginBottom: SPACING.space3,
  },
  rewardCard: {
    backgroundColor: COLORS.white,
    borderRadius: RADII.card,
    padding: SPACING.space3,
    marginBottom: SPACING.space3,
    ...SHADOWS.card,
  },
  rewardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.space1,
  },
  rewardStars: {
    fontSize: 18,
    fontWeight: '700',
    color: COLORS.accentGreen,
  },
  rewardBadge: {
    ...TYPOGRAPHY.micro,
    color: COLORS.textBlackSoft,
    fontWeight: '500',
  },
  rewardDesc: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
  },
  profileSection: {
    paddingHorizontal: SPACING.space4,
    paddingBottom: SPACING.space3,
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
    gap: SPACING.space2,
    marginBottom: SPACING.space6,
  },
  signOutBtn: {
    width: '100%',
  },
  resetBtn: {
    width: '100%',
  },
});
