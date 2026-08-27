package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeSetting
import com.example.data.model.InterestPeriod
import com.example.data.model.InterestType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sudhan_preferences", Context.MODE_PRIVATE)

    private val _isPinLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_PIN_ENABLED, false))
    val isPinLockEnabled: StateFlow<Boolean> = _isPinLockEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        try {
            AppLanguage.valueOf(prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.name)!!)
        } catch (e: Exception) {
            AppLanguage.ENGLISH
        }
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _appTheme = MutableStateFlow(
        try {
            AppThemeSetting.valueOf(prefs.getString(KEY_THEME, AppThemeSetting.SYSTEM.name)!!)
        } catch (e: Exception) {
            AppThemeSetting.SYSTEM
        }
    )
    val appTheme: StateFlow<AppThemeSetting> = _appTheme.asStateFlow()

    private val _defaultInterestType = MutableStateFlow(
        try {
            InterestType.valueOf(prefs.getString(KEY_DEFAULT_INTEREST_TYPE, InterestType.SIMPLE.name)!!)
        } catch (e: Exception) {
            InterestType.SIMPLE
        }
    )
    val defaultInterestType: StateFlow<InterestType> = _defaultInterestType.asStateFlow()

    private val _defaultInterestPeriod = MutableStateFlow(
        try {
            InterestPeriod.valueOf(prefs.getString(KEY_DEFAULT_INTEREST_PERIOD, InterestPeriod.MONTHLY.name)!!)
        } catch (e: Exception) {
            InterestPeriod.MONTHLY
        }
    )
    val defaultInterestPeriod: StateFlow<InterestPeriod> = _defaultInterestPeriod.asStateFlow()

    fun setPinLock(enabled: Boolean, pin: String? = null) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_PIN_ENABLED, enabled)
        if (pin != null) {
            editor.putString(KEY_PIN_HASH, hashPin(pin))
        }
        editor.apply()
        _isPinLockEnabled.value = enabled
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        if (storedHash.isEmpty()) return true
        return hashPin(pin) == storedHash
    }

    fun hasPinSet(): Boolean {
        return prefs.getString(KEY_PIN_HASH, "")?.isNotEmpty() == true
    }

    fun setBiometric(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        _appLanguage.value = language
    }

    fun setTheme(theme: AppThemeSetting) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _appTheme.value = theme
    }

    fun setDefaultInterestType(type: InterestType) {
        prefs.edit().putString(KEY_DEFAULT_INTEREST_TYPE, type.name).apply()
        _defaultInterestType.value = type
    }

    fun setDefaultInterestPeriod(period: InterestPeriod) {
        prefs.edit().putString(KEY_DEFAULT_INTEREST_PERIOD, period.name).apply()
        _defaultInterestPeriod.value = period
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_THEME = "app_theme"
        private const val KEY_DEFAULT_INTEREST_TYPE = "default_interest_type"
        private const val KEY_DEFAULT_INTEREST_PERIOD = "default_interest_period"
    }
}
