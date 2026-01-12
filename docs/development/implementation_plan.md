# Рефакторинг UI: Разделение на компоненты

Для улучшения читаемости и масштабируемости кода, мы разделим основные экраны на мелкие компоненты.

## Предлагаемые изменения

### [Компонент] UI Components
- **[NEW] [CommonComponents.kt](file:///c:/Users/ATLAS/GitHub/zerodata-chat/app/src/main/java/com/zerodata/chat/ui/components/CommonComponents.kt)**: Общие элементы, такие как стилизованные TopAppBar.
- **[NEW] [ChatComponents.kt](file:///c:/Users/ATLAS/GitHub/zerodata-chat/app/src/main/java/com/zerodata/chat/ui/components/ChatComponents.kt)**: `ChatListItem` и специфичные диалоги для хаба.
- **[NEW] [MessageComponents.kt](file:///c:/Users/ATLAS/GitHub/zerodata-chat/app/src/main/java/com/zerodata/chat/ui/components/MessageComponents.kt)**: `MessageInput` и структура списка сообщений.
- **[NEW] [LobbyComponents.kt](file:///c:/Users/ATLAS/GitHub/zerodata-chat/app/src/main/java/com/zerodata/chat/ui/components/LobbyComponents.kt)**: `LobbyUserItem`.

### [Компонент] UI Screens (Cleanup)
- **[MODIFY] [ChatListScreen.kt](file:///c:/Users/ATLAS/GitHub/zerodata-chat/app/src/main/java/com/zerodata/chat/ui/screens/ChatListScreen.kt)**: Использование новых компонентов, сокращение объема файла.
- **[MODIFY] [ChatScreen.kt](file:///c:/Users/ATLAS/GitHub/zerodata-chat/app/src/main/java/com/zerodata/chat/ui/screens/ChatScreen.kt)**: Аналогичная очистка.
- **[MODIFY] [LobbyScreen.kt](file:///c:/Users/ATLAS/GitHub/zerodata-chat/app/src/main/java/com/zerodata/chat/ui/screens/LobbyScreen.kt)**: Аналогичная очистка.

## План проверки

### Визуальная проверка
- Убедиться, что внешний вид приложения не изменился.
- Проверить корректность работы всех переходов и кнопок (Лобби, Копирование ID, Отправка сообщений).
