import React, { useState, useEffect } from 'react';
import { StyleSheet, View, Text, SafeAreaView, ScrollView, Pressable, ActivityIndicator } from 'react-native';
import { COLORS, TYPOGRAPHY, SPACING, SHADOWS, RADII } from '../styles/theme';
import Button from '../components/Button';
import { api } from '../services/api';

export default function SubscriptionPlansScreen({ onSubscriptionSuccess, onSignOut }) {
  const [plans, setPlans] = useState([]);
  const [selectedPlanId, setSelectedPlanId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    async function fetchPlans() {
      try {
        setIsLoading(true);
        setError('');
        const activePlans = await api.getActiveSubscriptionPlans();
        
        // Fallback plans if database returned empty array
        const defaultPlans = [
          { id: 'PLAN_1M', months: 1, price: 499, chatbotDailyLimit: 5 },
          { id: 'PLAN_3M', months: 3, price: 1299, chatbotDailyLimit: 10 },
          { id: 'PLAN_6M', months: 6, price: 2399, chatbotDailyLimit: 15 },
          { id: 'PLAN_12M', months: 12, price: 3999, chatbotDailyLimit: 20 },
        ];

        const finalPlans = Array.isArray(activePlans) && activePlans.length > 0 ? activePlans : defaultPlans;
        setPlans(finalPlans);
        
        // Default selection to 3M or first plan
        const defaultSelected = finalPlans.find(p => p.months === 3) || finalPlans[0];
        if (defaultSelected) {
          setSelectedPlanId(defaultSelected.id);
        }
      } catch (err) {
        console.error('Error fetching subscription plans:', err);
        setError('Failed to load subscription plans. Please check server connection.');
      } finally {
        setIsLoading(false);
      }
    }

    fetchPlans();
  }, []);

  const handleSubscribe = async () => {
    if (!selectedPlanId) return;

    setIsSubmitting(true);
    setError('');

    try {
      // Call POST /api/v1/subscription/start -> updates DB with user subscription
      const subResult = await api.startSubscription(selectedPlanId);
      setIsSubmitting(false);
      onSubscriptionSuccess(subResult);
    } catch (err) {
      console.error('Subscribe Error:', err);
      setError(err.message || 'Failed to start subscription.');
      setIsSubmitting(false);
    }
  };

  const getBadge = (months) => {
    if (months === 3) return { label: 'MOST POPULAR', color: COLORS.starbucksGreen, text: COLORS.white };
    if (months === 12) return { label: 'BEST VALUE', color: COLORS.gold, text: COLORS.houseGreen };
    return null;
  };

  if (isLoading) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color={COLORS.accentGreen} />
          <Text style={styles.loadingText}>Fetching Membership Plans...</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Header Section */}
        <View style={styles.header}>
          <Text style={styles.brandTitle}>Svaadanusaar</Text>
          <Text style={styles.heading}>Choose Your Plan</Text>
          <Text style={styles.subHeading}>
            Unlock full access to customized diet engine, health tracking, and AI chatbot assistance.
          </Text>
        </View>

        {error ? <Text style={styles.errorBanner}>{error}</Text> : null}

        {/* Plan Cards */}
        <View style={styles.plansContainer}>
          {plans.map((plan) => {
            const isSelected = selectedPlanId === plan.id;
            const badge = getBadge(plan.months);
            const monthlyEquivalent = Math.round(plan.price / plan.months);

            return (
              <Pressable
                key={plan.id}
                onPress={() => setSelectedPlanId(plan.id)}
                style={[
                  styles.planCard,
                  isSelected ? styles.planCardSelected : null,
                ]}
              >
                {badge ? (
                  <View style={[styles.badge, { backgroundColor: badge.color }]}>
                    <Text style={[styles.badgeText, { color: badge.text }]}>{badge.label}</Text>
                  </View>
                ) : null}

                <View style={styles.planHeader}>
                  <View>
                    <Text style={styles.planTitle}>
                      {plan.months} {plan.months === 1 ? 'Month' : 'Months'} Plan
                    </Text>
                    <Text style={styles.planSubTitle}>
                      ₹{monthlyEquivalent}/month
                    </Text>
                  </View>

                  <Text style={styles.planPrice}>₹{plan.price}</Text>
                </View>

                <View style={styles.divider} />

                <View style={styles.featuresList}>
                  <Text style={styles.featureItem}>
                    ✓ <Text style={{ fontWeight: '600' }}>{plan.chatbotDailyLimit} AI Assistant Queries</Text> / day
                  </Text>
                  <Text style={styles.featureItem}>
                    ✓ Personalized Nutrition Targets & Diet Plans
                  </Text>
                  <Text style={styles.featureItem}>
                    ✓ Weight Logging & Progress Analytics
                  </Text>
                </View>

                <View style={styles.radioContainer}>
                  <View style={[styles.radioOuter, isSelected ? styles.radioOuterSelected : null]}>
                    {isSelected ? <View style={styles.radioInner} /> : null}
                  </View>
                  <Text style={[styles.radioLabel, isSelected ? styles.radioLabelSelected : null]}>
                    {isSelected ? 'Selected Plan' : 'Tap to Select'}
                  </Text>
                </View>
              </Pressable>
            );
          })}
        </View>

        {/* Footer Actions */}
        <View style={styles.footer}>
          <Button
            title={isSubmitting ? 'Activating Subscription...' : 'Subscribe & Continue'}
            type="primary"
            disabled={!selectedPlanId || isSubmitting}
            onPress={handleSubscribe}
            style={styles.btn}
          />
          {onSignOut ? (
            <Pressable onPress={onSignOut} style={styles.signOutBtn}>
              <Text style={styles.signOutText}>Sign Out & Switch Account</Text>
            </Pressable>
          ) : null}
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
    paddingHorizontal: SPACING.outerGutter,
    paddingTop: SPACING.space4,
    paddingBottom: SPACING.space6,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: SPACING.space4,
  },
  loadingText: {
    marginTop: 12,
    color: COLORS.starbucksGreen,
    fontWeight: '600',
  },
  header: {
    alignItems: 'center',
    marginBottom: SPACING.space4,
  },
  brandTitle: {
    ...TYPOGRAPHY.titleSerif,
    fontSize: 28,
    color: COLORS.starbucksGreen,
    marginBottom: SPACING.space1,
  },
  heading: {
    ...TYPOGRAPHY.h1,
    color: COLORS.textBlack,
    textAlign: 'center',
    marginBottom: SPACING.space1,
  },
  subHeading: {
    ...TYPOGRAPHY.body,
    color: COLORS.textBlackSoft,
    textAlign: 'center',
    paddingHorizontal: SPACING.space2,
  },
  errorBanner: {
    ...TYPOGRAPHY.small,
    color: COLORS.errorRed,
    backgroundColor: COLORS.errorBgTint,
    padding: SPACING.space3,
    borderRadius: 8,
    marginBottom: SPACING.space4,
    fontWeight: '600',
    textAlign: 'center',
  },
  plansContainer: {
    gap: SPACING.space3,
    marginBottom: SPACING.space4,
  },
  planCard: {
    backgroundColor: COLORS.white,
    borderRadius: RADII.card,
    padding: SPACING.space4,
    borderWidth: 2,
    borderColor: COLORS.canvasCeramic,
    position: 'relative',
    ...SHADOWS.card,
  },
  planCardSelected: {
    borderColor: COLORS.starbucksGreen,
    backgroundColor: '#f4fbf7',
  },
  badge: {
    position: 'absolute',
    top: -12,
    right: 16,
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 12,
  },
  badgeText: {
    ...TYPOGRAPHY.micro,
    fontWeight: 'bold',
    fontSize: 10,
    letterSpacing: 0.5,
  },
  planHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  planTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: COLORS.textBlack,
  },
  planSubTitle: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
    marginTop: 2,
  },
  planPrice: {
    fontSize: 22,
    fontWeight: 'bold',
    color: COLORS.starbucksGreen,
  },
  divider: {
    height: 1,
    backgroundColor: COLORS.canvasCeramic,
    marginVertical: SPACING.space3,
  },
  featuresList: {
    gap: 6,
    marginBottom: SPACING.space3,
  },
  featureItem: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlack,
  },
  radioContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: SPACING.space1,
  },
  radioOuter: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: COLORS.textBlackSoft,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 8,
  },
  radioOuterSelected: {
    borderColor: COLORS.starbucksGreen,
  },
  radioInner: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: COLORS.starbucksGreen,
  },
  radioLabel: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
    fontWeight: '500',
  },
  radioLabelSelected: {
    color: COLORS.starbucksGreen,
    fontWeight: 'bold',
  },
  footer: {
    width: '100%',
    alignItems: 'center',
    gap: SPACING.space3,
  },
  btn: {
    width: '100%',
  },
  signOutBtn: {
    paddingVertical: SPACING.space2,
  },
  signOutText: {
    ...TYPOGRAPHY.small,
    color: COLORS.textBlackSoft,
    textDecorationLine: 'underline',
  },
});
