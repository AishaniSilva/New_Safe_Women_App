package lk.kiu.safewomen.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import lk.kiu.safewomen.data.local.AppDatabase
import lk.kiu.safewomen.data.model.ChatMessage
import lk.kiu.safewomen.data.model.EmergencyAlertLog
import lk.kiu.safewomen.data.model.Guardian
import lk.kiu.safewomen.data.model.TriggerMode
import lk.kiu.safewomen.data.repository.ChatRepository
import lk.kiu.safewomen.data.repository.GuardianRepository
import lk.kiu.safewomen.location.LocationTracker
import lk.kiu.safewomen.location.SafeLocationResult
import lk.kiu.safewomen.services.EmergencyTriggerCoordinator
import lk.kiu.safewomen.services.SafeWomenForegroundService
import lk.kiu.safewomen.telephony.SmsDispatcher
import lk.kiu.safewomen.utils.AuthManager
import lk.kiu.safewomen.utils.PreferenceManager

class MainViewModel(
    application: Application,
    private val repository: GuardianRepository,
    private val preferenceManager: PreferenceManager
) : AndroidViewModel(application) {

    val authManager = AuthManager(application)
    private val chatRepository = ChatRepository(AppDatabase.getInstance(application).chatMessageDao())
    private val locationTracker = LocationTracker(application)
    private val smsDispatcher = SmsDispatcher(application)

    // State flows
    val guardians: StateFlow<List<Guardian>> = repository.allGuardians
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = chatRepository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProtectionActive = MutableStateFlow(preferenceManager.isProtectionActive)
    val isProtectionActive: StateFlow<Boolean> = _isProtectionActive.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _triggerMode = MutableStateFlow(preferenceManager.triggerMode)
    val triggerMode: StateFlow<TriggerMode> = _triggerMode.asStateFlow()

    private val _triggerDurationMs = MutableStateFlow(preferenceManager.triggerDurationMs)
    val triggerDurationMs: StateFlow<Long> = _triggerDurationMs.asStateFlow()

    private val _customSmsTemplate = MutableStateFlow(preferenceManager.customSmsTemplate)
    val customSmsTemplate: StateFlow<String> = _customSmsTemplate.asStateFlow()

    private val _currentLocation = MutableStateFlow<SafeLocationResult?>(null)
    val currentLocation: StateFlow<SafeLocationResult?> = _currentLocation.asStateFlow()

    private val _alertLogs = MutableStateFlow<List<EmergencyAlertLog>>(emptyList())
    val alertLogs: StateFlow<List<EmergencyAlertLog>> = _alertLogs.asStateFlow()

    private val _isTriggering = MutableStateFlow(false)
    val isTriggering: StateFlow<Boolean> = _isTriggering.asStateFlow()

    private val _triggerProgress = MutableStateFlow(0f)
    val triggerProgress: StateFlow<Float> = _triggerProgress.asStateFlow()

    init {
        refreshLocation()
        refreshAccessibilityStatus()
    }

    fun refreshAccessibilityStatus() {
        _isAccessibilityEnabled.value = EmergencyTriggerCoordinator.isAccessibilityServiceEnabled(getApplication())
    }

    fun setTriggerProgress(progress: Float) {
        _triggerProgress.value = progress.coerceIn(0f, 1f)
    }

    fun toggleProtection(active: Boolean) {
        preferenceManager.isProtectionActive = active
        _isProtectionActive.value = active
        if (active) {
            SafeWomenForegroundService.startService(getApplication())
        } else {
            SafeWomenForegroundService.stopService(getApplication())
        }
    }

    fun setTriggerMode(mode: TriggerMode) {
        preferenceManager.triggerMode = mode
        _triggerMode.value = mode
    }

    fun setTriggerDuration(durationMs: Long) {
        preferenceManager.triggerDurationMs = durationMs
        _triggerDurationMs.value = durationMs
    }

    fun setCustomSmsTemplate(template: String) {
        preferenceManager.customSmsTemplate = template
        _customSmsTemplate.value = template
    }

    fun addGuardian(name: String, phoneNumber: String, relationship: String, isPrimary: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val guardian = Guardian(
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                relationship = relationship.trim().ifEmpty { "Guardian" },
                isPrimary = isPrimary
            )
            repository.insertGuardian(guardian)
        }
    }

    fun deleteGuardian(guardian: Guardian) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGuardian(guardian)
        }
    }

    fun sendChatMessage(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val msg = ChatMessage(
                senderName = "Passenger (${authManager.registeredPhoneNumber.ifBlank { "User" }})",
                senderRole = "PASSENGER",
                messageText = text.trim(),
                timestamp = System.currentTimeMillis()
            )
            chatRepository.sendMessage(msg)
        }
    }

    fun clearChat() {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.clearHistory()
        }
    }

    fun refreshLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = locationTracker.acquireCurrentLocation()
            _currentLocation.value = result
        }
    }

    fun triggerEmergencyAlert(triggerType: String) {
        if (_isTriggering.value) return
        _isTriggering.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var currentGuardians = repository.getAllGuardiansSync()
            if (currentGuardians.isEmpty()) {
                currentGuardians = listOf(
                    Guardian(name = "Primary Guardian", phoneNumber = "0776336982", relationship = "Guardian", isPrimary = true),
                    Guardian(name = "Secondary Guardian", phoneNumber = "0768361075", relationship = "Guardian", isPrimary = false)
                )
            }

            val loc = locationTracker.acquireCurrentLocation()
            _currentLocation.value = loc

            val result = smsDispatcher.dispatchEmergencyAlert(
                guardians = currentGuardians,
                latitude = loc.latitude,
                longitude = loc.longitude
            )

            // Post to in-app chat
            chatRepository.postSystemEmergencyBroadcast(loc.latitude, loc.longitude)

            val totalLatency = System.currentTimeMillis() - startTime
            val newLog = EmergencyAlertLog(
                timestamp = System.currentTimeMillis(),
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracyMeters = loc.accuracy,
                recipientsCount = result.sentCount,
                messagePayload = result.formattedMessage,
                isSuccess = result.isSuccess,
                triggerType = triggerType,
                triggerLatencyMs = 150L,
                totalLatencyMs = totalLatency
            )

            _alertLogs.update { listOf(newLog) + it }
            _isTriggering.value = false
            _triggerProgress.value = 0f
        }
    }
}
