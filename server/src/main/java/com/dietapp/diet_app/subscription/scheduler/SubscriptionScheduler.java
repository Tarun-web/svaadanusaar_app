package com.dietapp.diet_app.subscription.scheduler;

import com.dietapp.diet_app.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    /**
     * Runs every day at 2 AM (UTC) to mark expired subscriptions.
     * Cron expression: "0 0 2 * * ?" = every day at 02:00 AM
     * Format: second minute hour day month dayOfWeek
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void markExpiredSubscriptionsDaily() {
        try {
            log.info("Starting scheduled task: markExpiredSubscriptions");
            subscriptionService.markExpiredSubscriptions();
            log.info("Completed scheduled task: markExpiredSubscriptions");
        } catch (Exception e) {
            log.error("Error in markExpiredSubscriptionsDaily scheduler", e);
        }
    }

    // For testing: runs every 5 minutes (uncomment to test expiry logic in development)
    // Cron expression: "0 */5 * * * ?" = every 5 minutes
    // @Scheduled(cron = "0 */5 * * * ?")
    // public void markExpiredSubscriptionsTesting() {
    //     log.info("Testing scheduler: markExpiredSubscriptions");
    //     subscriptionService.markExpiredSubscriptions();
    // }

}



