package com.sor999.dalmoi.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sor999.dalmoi.BuildConfig
import com.sor999.dalmoi.data.local.AuthSession
import com.sor999.dalmoi.data.local.EncryptedTokenStore
import com.sor999.dalmoi.data.remote.ApiClient
import com.sor999.dalmoi.data.remote.createHttpClient
import com.sor999.dalmoi.data.repository.HttpBackendRepository
import com.sor999.dalmoi.domain.repository.BackendRepository
import com.sor999.dalmoi.platform.tracking.RunLocationApi
import com.sor999.dalmoi.platform.tracking.RunLocationService
import com.sor999.dalmoi.presentation.backend.BackendViewModel

class AppContainer(context: Context) {
    private val client = createHttpClient()
    private val auth = AuthSession(EncryptedTokenStore(context.applicationContext))
    private val api = ApiClient(client, BuildConfig.API_BASE_URL, auth)
    val repository: BackendRepository = HttpBackendRepository(api, auth)
    internal val runLocationApi = RunLocationApi(api, auth, BuildConfig.API_BASE_URL)
    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(BackendViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return BackendViewModel(repository) {
                RunLocationService.stop(context.applicationContext)
            } as T
        }
    }
}
