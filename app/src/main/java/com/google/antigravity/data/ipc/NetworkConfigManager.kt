package com.google.antigravity.data.ipc

import android.content.Context
import android.content.SharedPreferences

enum class DnsPreset(val id: String, val title: String, val primaryIp: String, val secondaryIp: String, val description: String) {
    SYSTEM("system", "SYSTEM / VPN (AUTO)", "", "", "Automatic DNS from active Android WiFi / VPN interface"),
    DNS_AI("dns_ai", "DNS-AI.RU", "192.144.59.14", "186.246.49.127", "Smart DNS for ChatGPT, Claude, Gemini & foreign AI access"),
    XBOX_DNS("xbox_dns", "XBOX-DNS", "111.88.96.54", "111.88.96.55", "Smart DNS proxy for unblocking geo-restricted services"),
    CLOUDFLARE("cloudflare", "CLOUDFLARE DNS", "1.1.1.1", "1.0.0.1", "Fast and secure global public resolver"),
    GOOGLE("google", "GOOGLE DNS", "8.8.8.8", "8.8.4.4", "Standard Google public resolver"),
    CUSTOM("custom", "CUSTOM DNS", "", "", "User-defined custom DNS nameserver IP")
}

class NetworkConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ag_network_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DNS_PRESET = "dns_preset"
        private const val KEY_CUSTOM_DNS = "custom_dns"
        private const val KEY_PROXY_ENABLED = "proxy_enabled"
        private const val KEY_PROXY_URL = "proxy_url"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    var selectedPreset: DnsPreset
        get() {
            val savedId = prefs.getString(KEY_DNS_PRESET, DnsPreset.SYSTEM.id)
            return DnsPreset.values().find { it.id == savedId } ?: DnsPreset.SYSTEM
        }
        set(value) {
            prefs.edit().putString(KEY_DNS_PRESET, value.id).apply()
        }

    var customDnsIp: String
        get() = prefs.getString(KEY_CUSTOM_DNS, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_DNS, value.trim()).apply()
        }

    var isProxyEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROXY_ENABLED, false) // Default disabled
        set(value) {
            prefs.edit().putBoolean(KEY_PROXY_ENABLED, value).apply()
        }

    var proxyUrl: String
        get() = prefs.getString(KEY_PROXY_URL, "http://127.0.0.1:8080") ?: "http://127.0.0.1:8080"
        set(value) {
            prefs.edit().putString(KEY_PROXY_URL, value.trim()).apply()
        }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
        }

    fun getEffectiveDnsServers(systemDns: List<String>): List<String> {
        return when (selectedPreset) {
            DnsPreset.SYSTEM -> systemDns.ifEmpty { listOf("1.1.1.1", "8.8.8.8") }
            DnsPreset.DNS_AI -> listOf(DnsPreset.DNS_AI.primaryIp, DnsPreset.DNS_AI.secondaryIp)
            DnsPreset.XBOX_DNS -> listOf(DnsPreset.XBOX_DNS.primaryIp, DnsPreset.XBOX_DNS.secondaryIp)
            DnsPreset.CLOUDFLARE -> listOf(DnsPreset.CLOUDFLARE.primaryIp, DnsPreset.CLOUDFLARE.secondaryIp)
            DnsPreset.GOOGLE -> listOf(DnsPreset.GOOGLE.primaryIp, DnsPreset.GOOGLE.secondaryIp)
            DnsPreset.CUSTOM -> {
                val ip = customDnsIp.trim()
                if (ip.isNotBlank()) listOf(ip) else systemDns.ifEmpty { listOf("1.1.1.1") }
            }
        }
    }
}
