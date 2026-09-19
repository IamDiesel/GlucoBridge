package de.glucobridge.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.glucobridge.domain.GlucoseRepository
import de.glucobridge.domain.Settings
import kotlinx.coroutines.launch

class GlucoseViewModel(private val repo: GlucoseRepository) : ViewModel() {
    val state = repo.state
    val settings = repo.settings
    val aleAvailable: Boolean get() = repo.aleAvailable

    fun login(email: String, password: String) = viewModelScope.launch {
        repo.login(email.trim(), password)
    }
    fun refresh() = viewModelScope.launch { repo.refreshNow() }
    fun updateSettings(transform: (Settings) -> Settings) = repo.updateSettings(transform)
    fun logout() = viewModelScope.launch { repo.logout() }
}
