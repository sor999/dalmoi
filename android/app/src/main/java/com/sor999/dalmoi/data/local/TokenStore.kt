package com.sor999.dalmoi.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class Tokens(val accessToken: String, val refreshToken: String)

internal interface TokenStore {
    val current: Tokens?
    fun save(tokens: Tokens)
    fun clear()
}

internal class EncryptedTokenStore(context: Context) : TokenStore {
    private val preferences = context.getSharedPreferences("auth_session", Context.MODE_PRIVATE)
    private val key: SecretKey by lazy {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey("dalmoi_session", null) as? SecretKey) ?: KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        ).apply {
            init(
                KeyGenParameterSpec.Builder(
                    "dalmoi_session",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build(),
            )
        }.generateKey()
    }

    @Volatile
    override var current: Tokens? = restore()
        private set

    @Synchronized
    override fun save(tokens: Tokens) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        val ciphertext = cipher.doFinal("${tokens.accessToken}\n${tokens.refreshToken}".toByteArray(Charsets.UTF_8))
        preferences.edit().putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("data", Base64.encodeToString(ciphertext, Base64.NO_WRAP)).apply()
        current = tokens
    }

    @Synchronized
    override fun clear() {
        current = null
        preferences.edit().clear().apply()
    }

    private fun restore(): Tokens? {
        val encrypted = preferences.getString("data", null) ?: return null
        return try {
            val iv = Base64.decode(preferences.getString("iv", null), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            }
            val parts = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)).toString(Charsets.UTF_8).split('\n')
            if (parts.size == 2 && parts.all { it.isNotBlank() }) Tokens(parts[0], parts[1]) else null
        } catch (_: Exception) {
            preferences.edit().clear().apply()
            null
        }
    }
}

internal class AuthSession(private val store: TokenStore) {
    private val mutableSignedIn = MutableStateFlow(store.current != null)
    val signedIn = mutableSignedIn.asStateFlow()
    val current get() = store.current

    @Synchronized
    fun save(tokens: Tokens) {
        store.save(tokens)
        mutableSignedIn.value = true
    }

    @Synchronized
    fun clearIfCurrent(expected: Tokens?) {
        if (store.current == expected) {
            store.clear()
            mutableSignedIn.value = false
        }
    }

    @Synchronized
    fun replaceIfCurrent(expected: Tokens, replacement: Tokens): Boolean {
        if (store.current != expected) return false
        save(replacement)
        return true
    }
}
