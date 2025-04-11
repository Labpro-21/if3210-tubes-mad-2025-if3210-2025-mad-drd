package com.example.purrytify.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.repository.AuthRepository
import com.example.purrytify.data.repository.ProfileRepository
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.domain.model.Profile
import com.example.purrytify.domain.util.Resource
import com.example.purrytify.domain.auth.AuthStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val songRepository: SongRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog
    
    private val _isUpdateLoading = MutableStateFlow(false)
    val isUpdateLoading: StateFlow<Boolean> = _isUpdateLoading
    
    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError

    // Stats
    private val _songsCount = MutableStateFlow(0)
    val songsCount: Int get() = _songsCount.value

    private val _likedSongsCount = MutableStateFlow(0)
    val likedSongsCount: Int get() = _likedSongsCount.value

    private val _listenedSongsCount = MutableStateFlow(0)
    val listenedSongsCount: Int get() = _listenedSongsCount.value

    init {
        loadProfile()
        loadStats()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            profileRepository.getProfile().collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.value = ProfileUiState.Success(result.data)
                    is Resource.Error -> AuthStateManager.triggerLogout()
                    is Resource.Loading -> _uiState.value = ProfileUiState.Loading
                }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Load total songs count
            songRepository.getAllSongs().collectLatest { songs ->
                _songsCount.value = songs.size
            }
        }

        viewModelScope.launch {
            // Load liked songs count
            songRepository.getLikedSongs().collectLatest { songs ->
                _likedSongsCount.value = songs.size
            }
        }

        // For listened songs (recently played), get a count of songs with lastPlayedAt not null
        viewModelScope.launch {
            songRepository.getRecentlyPlayedSongs().collectLatest { songs ->
                _listenedSongsCount.value = songs.size
            }
        }
    }

    fun onLogoutClick() {
        _showLogoutDialog.value = true
    }

    fun dismissLogoutDialog() {
        _showLogoutDialog.value = false
    }

    fun onEditProfileClick() {
        _updateError.value = null
        _isUpdateLoading.value = false
        _showEditDialog.value = true
    }

    fun dismissEditDialog() {
        _showEditDialog.value = false
        _updateError.value = null
        _isUpdateLoading.value = false
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearTokens()
            _showLogoutDialog.value = false
        }
    }

    fun updateProfile(username: String, email: String, location: String) {
        // Form validation is done in the UI
        viewModelScope.launch {
            _isUpdateLoading.value = true
            _updateError.value = null
            
            profileRepository.updateProfile(username, email, location).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        loadProfile()
                        _isUpdateLoading.value = false
                        _showEditDialog.value = false
                    }
                    is Resource.Error -> {
                        _updateError.value = result.message
                        _isUpdateLoading.value = false
                    }
                    is Resource.Loading -> {
                        _isUpdateLoading.value = true
                    }
                }
            }
        }
    }

    fun updateProfilePicture(file: File) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            profileRepository.updateProfilePicture(file).collect { result ->
                when (result) {
                    is Resource.Success -> loadProfile()
                    is Resource.Error -> _uiState.value = ProfileUiState.Error(result.message)
                    is Resource.Loading -> _uiState.value = ProfileUiState.Loading
                }
            }
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: Profile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}