package com.julcer26.tasave.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.julcer26.tasave.data.LocalStorageHelper

/**
 * Privacy-respecting anonymous usage analytics wrapper.
 * Logs only screen/feature interaction names (never amounts, names, or payment profile data).
 * Safely no-ops when Firebase is not yet initialized (e.g. unit tests or missing google-services.json)
 * or when the user disables anonymous statistics in Settings.
 */
object AnalyticsTracker {

    private fun getAnalyticsIfReady(context: Context): FirebaseAnalytics? {
        val storage = LocalStorageHelper(context)
        if (!storage.isAnalyticsEnabled()) return null
        return runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) return null
            FirebaseAnalytics.getInstance(context)
        }.getOrNull()
    }

    fun setCollectionEnabled(context: Context, enabled: Boolean) {
        LocalStorageHelper(context).setAnalyticsEnabled(enabled)
        runCatching {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled)
            }
        }
    }

    fun logTabOpened(context: Context, tabName: String) {
        logEvent(context, "tasave_tab_opened", mapOf("tab_name" to tabName))
    }

    fun logUtilToolOpened(context: Context, toolName: String) {
        logEvent(context, "tasave_util_opened", mapOf("tool_name" to toolName))
    }

    fun logConverterRateSelected(context: Context, rateKey: String) {
        logEvent(context, "tasave_rate_selected", mapOf("rate_key" to rateKey))
    }

    fun logCalendarOpened(context: Context) {
        logEvent(context, "tasave_calendar_opened")
    }

    fun logCalendarDaySelected(context: Context, isHoliday: Boolean) {
        logEvent(
            context,
            "tasave_calendar_day_selected",
            mapOf("is_bank_holiday" to if (isHoliday) "true" else "false")
        )
    }

    fun logAction(context: Context, actionName: String) {
        logEvent(context, "tasave_action", mapOf("action_name" to actionName))
    }

    private fun logEvent(
        context: Context,
        eventName: String,
        params: Map<String, String> = emptyMap()
    ) {
        val analytics = getAnalyticsIfReady(context) ?: return
        runCatching {
            val bundle = Bundle().apply {
                params.forEach { (k, v) -> putString(k, v) }
            }
            analytics.logEvent(eventName, bundle)
        }
    }
}
