package io.github.nosorae.aiopic.libs.keyvault.api

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64

// 같은 파일을 여는 DataStore 가 한 프로세스에 둘이면 DataStore 가 실패하므로 Context 확장 하나로 공유한다.
private val Context.keyStore: DataStore<Preferences> by preferencesDataStore(KeystoreKeyVault.FILE_NAME)

/**
 * 키를 Android Keystore 의 AES-GCM 키로 암호화해 DataStore 에 둔다. 제공사 이름이 항목 이름이다.
 * 암호화 키는 Keystore 밖으로 나오지 않으므로 저장 파일이 백업이나 다른 기기로 옮겨 가도 풀 수 없다.
 */
public class KeystoreKeyVault(context: Context) : KeyVault {
    private val store = context.applicationContext.keyStore

    override suspend fun get(provider: String): String? {
        val stored = store.data.first()[stringPreferencesKey(provider)] ?: return null
        val bytes = Base64.decode(stored)
        return try {
            cipher(Cipher.DECRYPT_MODE, GCMParameterSpec(TAG_BITS, bytes, 0, IV_SIZE))
                .doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE)
                .decodeToString()
        } catch (_: GeneralSecurityException) {
            // 백업 복원 등으로 Keystore 키가 바뀌면 풀 수 없다. 키가 없는 것으로 보고 사용자가 다시 넣게 한다.
            null
        }
    }

    override suspend fun put(provider: String, key: String) {
        val cipher = cipher(Cipher.ENCRYPT_MODE, null)
        val stored = Base64.encode(cipher.iv + cipher.doFinal(key.encodeToByteArray()))
        store.edit { it[stringPreferencesKey(provider)] = stored }
    }

    override suspend fun delete(provider: String) {
        store.edit { it.remove(stringPreferencesKey(provider)) }
    }

    private fun cipher(mode: Int, spec: GCMParameterSpec?): Cipher = Cipher.getInstance(TRANSFORMATION).apply { init(mode, secretKey(), spec) }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as SecretKey?)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    internal companion object {
        const val FILE_NAME = "keyvault"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "io.github.nosorae.aiopic.keyvault"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }
}
