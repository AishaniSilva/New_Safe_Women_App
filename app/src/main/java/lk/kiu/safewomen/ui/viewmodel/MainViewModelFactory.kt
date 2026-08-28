package lk.kiu.safewomen.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import lk.kiu.safewomen.data.repository.GuardianRepository
import lk.kiu.safewomen.utils.PreferenceManager

class MainViewModelFactory(
    private val application: Application,
    private val repository: GuardianRepository,
    private val preferenceManager: PreferenceManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application, repository, preferenceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
