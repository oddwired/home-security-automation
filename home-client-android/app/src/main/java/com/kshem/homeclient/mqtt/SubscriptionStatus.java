package com.kshem.homeclient.mqtt;

public enum SubscriptionStatus {
        SUBSCRIBED,
        SUBSCRIBING,
        UNSUBSCRIBED,
        UNSUBSCRIBING,
        AWAITING_UNSUBSCRIPTION,
        PUBLISH_ONLY
};