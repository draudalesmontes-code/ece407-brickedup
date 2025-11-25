package com.cs407.brickcollector.models

import androidx.activity.result.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.brickcollector.api.ApiService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserState(
    val id: Int = 0, val username: String = "", val uid: String = ""
)

/**
 * ViewModel for handling users with firebase
 */
class UserViewModels : ViewModel() {
    private val _userState = MutableStateFlow(UserState())
    private val auth: FirebaseAuth = Firebase.auth
    val userState = _userState.asStateFlow()

    init {
        auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user == null) {
                setUser(UserState())
            }
        }
    }

    fun setUser(state: UserState) {
        _userState.update {
            state
        }
    }
}

/**
 * ViewModel for handling searches of Lego sets, uses api
 */
class SearchViewModel : ViewModel() {
    private val _searchResults = MutableStateFlow<List<LegoSet>>(emptyList())
    val searchResults: StateFlow<List<LegoSet>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun searchSets(query: String, apiKey: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // Call the searchSets method from the ApiService
            /*
            try {
                val response =
                    ApiService.retrofit.searchSets(
                        query,
                        apiKey
                    )
                _searchResults.value = response.results
            } catch (e: Exception) {
                _error.value = "${e.message()}"
            } finally {
                _isLoading.value = false
            }
            */
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
        _error.value = null
    }
}

/**
 * ViewModel for managing want_list and sell_list
 */
class ListsViewModel(private val legoDao: LegoDao) : ViewModel() {
    private val _wantlist = MutableStateFlow<List<LegoSet>>(emptyList())
    val wishlist: StateFlow<List<LegoSet>> = _wantlist

    private val _sellList = MutableStateFlow<List<LegoSet>>(emptyList())
    val sellList: StateFlow<List<LegoSet>> = _sellList


    fun addSetToUserCollection(userId: Int, set: LegoSet, listType: String) {
        viewModelScope.launch {
            // Insert the set details into the LegoSet table first
            legoDao.insertSets(listOf(set))
            // Create the relationship in the cross-reference table
            val crossRef = UserSetCrossRef(userId = userId, setId = set.setId, listType = listType)
            legoDao.addUserSetCrossRef(crossRef)
            // Refresh the appropriate list
            if (listType == "WANT_LIST") {
                legoDao.insertWantListSet(userId, set)
            } else {
                legoDao.insertSellListSet(userId, set)
            }
        }
    }
}