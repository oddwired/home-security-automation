package com.kshem.homesecurity.mqtt;

public enum SubscriptionStatus {
        SUBSCRIBED,
        SUBSCRIBING,
        UNSUBSCRIBED,
        UNSUBSCRIBING,
        AWAITING_UNSUBSCRIPTION,
        PUBLISH_ONLY
};