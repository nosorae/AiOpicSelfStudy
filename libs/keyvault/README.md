# keyvault

사용자가 넣은 API 키를 기기 보안 저장소에 두는 라이브러리다. 제공사 이름(문자열)마다 키를 따로 저장해서, 한 제공사 키를 지워도 다른 키는 남는다. 프로젝트의 다른 모듈을 의존하지 않는다.

| 플랫폼 | 구현 | 저장 방식 |
|---|---|---|
| Android | `KeystoreKeyVault(context)` | Keystore 의 AES-GCM 키로 암호화해 DataStore 에 둔다 |
| iOS | `KeychainKeyVault()` | Keychain, 이 기기에서만 풀리게 둔다 |

```kotlin
val vault: KeyVault = KeystoreKeyVault(context)
vault.put("gemini", key)
val saved: String? = vault.get("gemini")
vault.delete("gemini")
```

키는 다른 기기에서 풀 수 없으므로 앱이 자동 백업에서 빼야 한다([ADR 0001](../../docs/adr/0001-서버-없이-사용자-API-키.md)).

## 테스트

`commonTest` 의 `KeyVaultContractTest` 가 계약이다. 가짜 구현(`FakeKeyVault`), Android 계측 테스트, iOS 시뮬레이터 테스트가 같은 계약을 돈다. Keystore 는 기기에만 있어서 Android 쪽은 `androidDeviceTest` 로 확인하고, Keychain 은 부팅된 시뮬레이터에서만 열려서 iOS 쪽은 iPhone 17 시뮬레이터 안에서 돈다.
