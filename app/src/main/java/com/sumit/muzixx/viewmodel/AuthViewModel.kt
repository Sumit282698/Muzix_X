package com.sumit.muzixx.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.sumit.muzixx.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUser get() = auth.currentUser
    var firestorePlaylistsCount by mutableIntStateOf(0)
        private set
    var firestoreSongsCount by mutableIntStateOf(0)
        private set
    var firestoreTotalSongsHeard by mutableIntStateOf(0)
        private set
    var firestoreMonthlySongsHeard by mutableIntStateOf(0)
        private set
    var firestoreYearlySongsHeard by mutableIntStateOf(0)
        private set
    var firestoreTotalPlaySeconds by mutableLongStateOf(0L)
        private set
    var firestoreMonthlyPlaySeconds by mutableLongStateOf(0L)
        private set
    var firestoreYearlyPlaySeconds by mutableLongStateOf(0L)
        private set

    init {
        currentUser?.uid?.let { loggedInUid ->
            fetchAndSetUserData(loggedInUid)
        }
    }

    fun authenticateWithEmailPassword(
        email: String,
        password: String,
        defaultDisplayName: String,
        defaultGender: String,
        localSongsHeard: Int,
        localMonthlySongs: Int,
        localYearlySongs: Int,
        localTotalSeconds: Long,
        localMonthlySeconds: Long,
        localYearlySeconds: Long
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                try {
                    val authResult = auth.signInWithEmailAndPassword(email, password).await()
                    val user = authResult.user ?: throw Exception("Authentication returned an empty session.")

                    fetchAndSetUserData(user.uid)
                    _authState.value = AuthState.Success
                } catch (loginException: Exception) {
                    val isUserNotFound = loginException is FirebaseAuthInvalidUserException
                    val isInvalidCreds = loginException is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

                    if (isUserNotFound || isInvalidCreds) {
                        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                        val user = authResult.user ?: throw Exception("Registration returned an empty session.")
                        val initialName = defaultDisplayName.ifBlank { "MuzixX Listener" }
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(initialName)
                            .build()
                        user.updateProfile(profileUpdates).await()

                        val userDocRef = firestore.collection("users").document(user.uid)
                        val newUser = UserProfile(
                            uid = user.uid,
                            name = initialName,
                            username = (user.email?.substringBefore("@") ?: "user_${user.uid.take(5)}").lowercase(),
                            email = user.email ?: "",
                            totalSongsHeard = localSongsHeard,
                            monthlySongsHeard = localMonthlySongs,
                            yearlySongsHeard = localYearlySongs,
                            totalPlaySeconds = localTotalSeconds,
                            monthlyPlaySeconds = localMonthlySeconds,
                            yearlyPlaySeconds = localYearlySeconds
                        )

                        val userMap = newUser.toMap().toMutableMap()
                        userMap["gender"] = defaultGender

                        userDocRef.set(userMap).await()

                        updateLiveStates(
                            totalSongs = 0, playlists = 0,
                            totalHeard = localSongsHeard, monthlyHeard = localMonthlySongs, yearlyHeard = localYearlySongs,
                            totalSec = localTotalSeconds, monthlySec = localMonthlySeconds, yearlySec = localYearlySeconds
                        )
                        _authState.value = AuthState.Success
                    } else {
                        throw loginException
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Authentication operation failed.")
            }
        }
    }

    private fun fetchAndSetUserData(uid: String) {
        fetchUserProfile(uid) { profile ->
            profile?.let {
                updateLiveStates(
                    totalSongs = 0,
                    playlists = 0,
                    totalHeard = it.totalSongsHeard,
                    monthlyHeard = it.monthlySongsHeard,
                    yearlyHeard = it.yearlySongsHeard,
                    totalSec = it.totalPlaySeconds,
                    monthlySec = it.monthlyPlaySeconds,
                    yearlySec = it.yearlyPlaySeconds
                )
            }
        }
    }

    private fun updateLiveStates(
        totalSongs: Int, playlists: Int, totalHeard: Int, monthlyHeard: Int, yearlyHeard: Int,
        totalSec: Long, monthlySec: Long, yearlySec: Long
    ) {
        firestoreSongsCount = totalSongs
        firestorePlaylistsCount = playlists
        firestoreTotalSongsHeard = totalHeard
        firestoreMonthlySongsHeard = monthlyHeard
        firestoreYearlySongsHeard = yearlyHeard
        firestoreTotalPlaySeconds = totalSec
        firestoreMonthlyPlaySeconds = monthlySec
        firestoreYearlyPlaySeconds = yearlySec
    }

    fun fetchUserProfile(uid: String, onResult: (UserProfile?) -> Unit) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(uid).get().await()
                val profile = document.toObject(UserProfile::class.java)
                withContext(Dispatchers.Main) {
                    onResult(profile)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            }
        }
    }

    fun updateProfileData(newName: String, newGender: String) {
        val uid = currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                currentUser?.updateProfile(profileUpdates)?.await()

                val updates = mapOf(
                    "name" to newName,
                    "gender" to newGender
                )
                firestore.collection("users").document(uid).update(updates).await()

                fetchAndSetUserData(uid)
            } catch (_: Exception) {
            }
        }
    }

    fun saveDataOnExitOrCrash(
        playlistsCount: Int,
        songsCount: Int,
        totalHeard: Int,
        monthlyHeard: Int,
        yearlyHeard: Int,
        totalSec: Long,
        monthlySec: Long,
        yearlySec: Long
    ) {
        val uid = currentUser?.uid ?: return

        val absoluteUpdates = mapOf(
            "playlistsCount" to playlistsCount,
            "songsCount" to songsCount,
            "totalSongsHeard" to totalHeard,
            "monthlySongsHeard" to monthlyHeard,
            "yearlySongsHeard" to yearlyHeard,
            "totalPlaySeconds" to totalSec,
            "monthlyPlaySeconds" to monthlySec,
            "yearlyPlaySeconds" to yearlySec
        )

        firestore.collection("users")
            .document(uid)
            .update(absoluteUpdates)
    }

    fun logout() {
        auth.signOut()
        updateLiveStates(0, 0, 0, 0, 0, 0L, 0L, 0L)
        resetState()
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object Success : AuthState
    data class Error(val message: String) : AuthState
}