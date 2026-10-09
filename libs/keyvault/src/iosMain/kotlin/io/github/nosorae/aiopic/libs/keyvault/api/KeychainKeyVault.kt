package io.github.nosorae.aiopic.libs.keyvault.api

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * 키를 Keychain 일반 암호 항목으로 둔다. 제공사 이름이 항목의 account 다.
 * 접근 등급은 AfterFirstUnlockThisDeviceOnly 다. 기기를 한 번 연 뒤 앞으로 올 때 대기열을 비우는 동안에도 읽혀야 하고,
 * 백업으로 다른 기기에 키가 옮겨 가면 안 된다.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public class KeychainKeyVault : KeyVault {
    override suspend fun get(provider: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(provider, kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne) {
            SecItemCopyMatching(it, result.ptr)
        }
        when (status) {
            errSecSuccess -> (CFBridgingRelease(result.value) as NSData).let { it.bytes?.readBytes(it.length.toInt()) ?: ByteArray(0) }.decodeToString()
            errSecItemNotFound -> null
            else -> error("Keychain 읽기 실패: $status")
        }
    }

    override suspend fun put(provider: String, key: String) {
        delete(provider)
        val bytes = key.encodeToByteArray()
        val status = memScoped {
            val data = CFBridgingRetain(NSData.create(bytes = allocArrayOf(bytes), length = bytes.size.toULong()))
            try {
                withQuery(provider, kSecValueData to data, kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly) {
                    SecItemAdd(it, null)
                }
            } finally {
                CFRelease(data)
            }
        }
        check(status == errSecSuccess) { "Keychain 저장 실패: $status" }
    }

    override suspend fun delete(provider: String) {
        val status = withQuery(provider) { SecItemDelete(it) }
        check(status == errSecSuccess || status == errSecItemNotFound) { "Keychain 삭제 실패: $status" }
    }

    /** 이 앱의 서비스와 제공사로 찾는 질의를 만들어 넘기고, 끝나면 만든 CF 객체를 놓는다. */
    private inline fun <T> withQuery(provider: String, vararg extra: Pair<CFStringRef?, CFTypeRef?>, block: (CFDictionaryRef?) -> T): T {
        val service = CFBridgingRetain(SERVICE)
        val account = CFBridgingRetain(provider)
        val query = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, service)
            CFDictionaryAddValue(query, kSecAttrAccount, account)
            extra.forEach { (name, value) -> CFDictionaryAddValue(query, name, value) }
            return block(query)
        } finally {
            CFRelease(query)
            CFRelease(account)
            CFRelease(service)
        }
    }

    private companion object {
        const val SERVICE = "io.github.nosorae.aiopic.keyvault"
    }
}
