package com.zerodata.chat.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.zerodata.chat.model.MessageStatus;
import com.zerodata.chat.model.MessageType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ChatDao_Impl implements ChatDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChatEntity> __insertionAdapterOfChatEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateUnreadCount;

  private final SharedSQLiteStatement __preparedStmtOfDeleteChat;

  public ChatDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChatEntity = new EntityInsertionAdapter<ChatEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `chats` (`id`,`name`,`unreadCount`,`avatarUrl`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getUnreadCount());
        if (entity.getAvatarUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getAvatarUrl());
        }
      }
    };
    this.__preparedStmtOfUpdateUnreadCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE chats SET unreadCount = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteChat = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chats WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertChat(final ChatEntity chat, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatEntity.insert(chat);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateUnreadCount(final String chatId, final int count,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateUnreadCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, count);
        _argIndex = 2;
        _stmt.bindString(_argIndex, chatId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateUnreadCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteChat(final String chatId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteChat.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, chatId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteChat.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChatWithLastMessage>> getChatsWithLastMessageFlow() {
    final String _sql = "SELECT * FROM chats";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"messages",
        "chats"}, new Callable<List<ChatWithLastMessage>>() {
      @Override
      @NonNull
      public List<ChatWithLastMessage> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
            final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
            final ArrayMap<String, ArrayList<MessageEntity>> _collectionAllMessages = new ArrayMap<String, ArrayList<MessageEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionAllMessages.containsKey(_tmpKey)) {
                _collectionAllMessages.put(_tmpKey, new ArrayList<MessageEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipmessagesAscomZerodataChatDatabaseMessageEntity(_collectionAllMessages);
            final List<ChatWithLastMessage> _result = new ArrayList<ChatWithLastMessage>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final ChatWithLastMessage _item;
              final ChatEntity _tmpChat;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpName;
              _tmpName = _cursor.getString(_cursorIndexOfName);
              final int _tmpUnreadCount;
              _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
              final String _tmpAvatarUrl;
              if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
                _tmpAvatarUrl = null;
              } else {
                _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
              }
              _tmpChat = new ChatEntity(_tmpId,_tmpName,_tmpUnreadCount,_tmpAvatarUrl);
              final ArrayList<MessageEntity> _tmpAllMessagesCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpAllMessagesCollection = _collectionAllMessages.get(_tmpKey_1);
              _item = new ChatWithLastMessage(_tmpChat,_tmpAllMessagesCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private MessageStatus __MessageStatus_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "SENDING": return MessageStatus.SENDING;
      case "SENT": return MessageStatus.SENT;
      case "DELIVERED": return MessageStatus.DELIVERED;
      case "READ": return MessageStatus.READ;
      case "ERROR": return MessageStatus.ERROR;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private MessageType __MessageType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "TEXT": return MessageType.TEXT;
      case "IMAGE": return MessageType.IMAGE;
      case "SYSTEM": return MessageType.SYSTEM;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private void __fetchRelationshipmessagesAscomZerodataChatDatabaseMessageEntity(
      @NonNull final ArrayMap<String, ArrayList<MessageEntity>> _map) {
    final Set<String> __mapKeySet = _map.keySet();
    if (__mapKeySet.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchArrayMap(_map, true, (map) -> {
        __fetchRelationshipmessagesAscomZerodataChatDatabaseMessageEntity(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `id`,`chatId`,`senderId`,`receiverId`,`text`,`timestamp`,`status`,`type` FROM `messages` WHERE `chatId` IN (");
    final int _inputSize = __mapKeySet.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : __mapKeySet) {
      _stmt.bindString(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "chatId");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfChatId = 1;
      final int _cursorIndexOfSenderId = 2;
      final int _cursorIndexOfReceiverId = 3;
      final int _cursorIndexOfText = 4;
      final int _cursorIndexOfTimestamp = 5;
      final int _cursorIndexOfStatus = 6;
      final int _cursorIndexOfType = 7;
      while (_cursor.moveToNext()) {
        final String _tmpKey;
        _tmpKey = _cursor.getString(_itemKeyIndex);
        final ArrayList<MessageEntity> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final MessageEntity _item_1;
          final String _tmpId;
          _tmpId = _cursor.getString(_cursorIndexOfId);
          final String _tmpChatId;
          _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
          final String _tmpSenderId;
          _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
          final String _tmpReceiverId;
          _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId);
          final String _tmpText;
          _tmpText = _cursor.getString(_cursorIndexOfText);
          final long _tmpTimestamp;
          _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
          final MessageStatus _tmpStatus;
          _tmpStatus = __MessageStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
          final MessageType _tmpType;
          _tmpType = __MessageType_stringToEnum(_cursor.getString(_cursorIndexOfType));
          _item_1 = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpReceiverId,_tmpText,_tmpTimestamp,_tmpStatus,_tmpType);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
