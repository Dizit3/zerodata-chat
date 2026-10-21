# ZeroData Chat

Android MQTT messenger (Jetpack Compose). Not an LLM agent. Work from this folder as the workspace root.

## Layers

Compose UI → ViewModel (`StateFlow`) → `ChatRepository` only. Do not call `MqttManager` or `AppDatabase` from UI or ViewModels.

- 1:1 chat ids: `ChatUtils.getCanonicalChatId(user1, user2)` (sorted ids joined by `_`). Never mint random personal chat ids.
- Room `Entity` types stay in the repository. Map to `Chat` / `Message` before ViewModels.
- MQTT topics live in `app/src/main/java/com/zerodata/chat/util/Constants.kt`. Payloads: `MqttPayloadMapper` / `CompactMessageWire`.
- Do not remove `GlobalExceptionHandler` or change `CrashActivity` unless the user asks.
- Dark Material3 (`0xFF121212`). `AddChatDialog` is a required hub action.

## Where to look

- Invariants: `.cursor/rules/*.mdc`
- Obtainium GitHub pre-release after a user-facing feature: `.cursor/skills/obtainium-prerelease/`
- Network/repo changes: read `ChatRepositoryImpl.kt` first
