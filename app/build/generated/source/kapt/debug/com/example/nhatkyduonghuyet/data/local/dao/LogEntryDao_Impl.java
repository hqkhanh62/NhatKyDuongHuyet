package com.example.nhatkyduonghuyet.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import com.example.nhatkyduonghuyet.data.model.AdvancedStatsEntity;
import com.example.nhatkyduonghuyet.data.model.DailyAvgRow;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LogEntryDao_Impl implements LogEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LogEntry> __insertionAdapterOfLogEntry;

  private final EntityDeletionOrUpdateAdapter<LogEntry> __deletionAdapterOfLogEntry;

  private final EntityDeletionOrUpdateAdapter<LogEntry> __updateAdapterOfLogEntry;

  private final EntityUpsertionAdapter<LogEntry> __upsertionAdapterOfLogEntry;

  public LogEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLogEntry = new EntityInsertionAdapter<LogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `log_entries` (`id`,`date`,`session`,`medType`,`dose`,`time`,`value`,`bgBefore`,`bgAfter`,`bpSys`,`bpDia`,`heartRate`,`note`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LogEntry entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        if (entity.getSession() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getSession());
        }
        if (entity.getMedType() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMedType());
        }
        if (entity.getDose() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDose());
        }
        if (entity.getTime() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getTime());
        }
        statement.bindLong(7, entity.getValue());
        if (entity.getBgBefore() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getBgBefore());
        }
        if (entity.getBgAfter() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getBgAfter());
        }
        if (entity.getBpSys() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getBpSys());
        }
        if (entity.getBpDia() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getBpDia());
        }
        if (entity.getHeartRate() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getHeartRate());
        }
        if (entity.getNote() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getNote());
        }
      }
    };
    this.__deletionAdapterOfLogEntry = new EntityDeletionOrUpdateAdapter<LogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `log_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LogEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfLogEntry = new EntityDeletionOrUpdateAdapter<LogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `log_entries` SET `id` = ?,`date` = ?,`session` = ?,`medType` = ?,`dose` = ?,`time` = ?,`value` = ?,`bgBefore` = ?,`bgAfter` = ?,`bpSys` = ?,`bpDia` = ?,`heartRate` = ?,`note` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LogEntry entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        if (entity.getSession() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getSession());
        }
        if (entity.getMedType() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMedType());
        }
        if (entity.getDose() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDose());
        }
        if (entity.getTime() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getTime());
        }
        statement.bindLong(7, entity.getValue());
        if (entity.getBgBefore() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getBgBefore());
        }
        if (entity.getBgAfter() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getBgAfter());
        }
        if (entity.getBpSys() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getBpSys());
        }
        if (entity.getBpDia() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getBpDia());
        }
        if (entity.getHeartRate() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getHeartRate());
        }
        if (entity.getNote() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getNote());
        }
        statement.bindLong(14, entity.getId());
      }
    };
    this.__upsertionAdapterOfLogEntry = new EntityUpsertionAdapter<LogEntry>(new EntityInsertionAdapter<LogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `log_entries` (`id`,`date`,`session`,`medType`,`dose`,`time`,`value`,`bgBefore`,`bgAfter`,`bpSys`,`bpDia`,`heartRate`,`note`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LogEntry entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        if (entity.getSession() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getSession());
        }
        if (entity.getMedType() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMedType());
        }
        if (entity.getDose() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDose());
        }
        if (entity.getTime() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getTime());
        }
        statement.bindLong(7, entity.getValue());
        if (entity.getBgBefore() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getBgBefore());
        }
        if (entity.getBgAfter() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getBgAfter());
        }
        if (entity.getBpSys() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getBpSys());
        }
        if (entity.getBpDia() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getBpDia());
        }
        if (entity.getHeartRate() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getHeartRate());
        }
        if (entity.getNote() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getNote());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<LogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `log_entries` SET `id` = ?,`date` = ?,`session` = ?,`medType` = ?,`dose` = ?,`time` = ?,`value` = ?,`bgBefore` = ?,`bgAfter` = ?,`bpSys` = ?,`bpDia` = ?,`heartRate` = ?,`note` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LogEntry entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        if (entity.getSession() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getSession());
        }
        if (entity.getMedType() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMedType());
        }
        if (entity.getDose() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDose());
        }
        if (entity.getTime() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getTime());
        }
        statement.bindLong(7, entity.getValue());
        if (entity.getBgBefore() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getBgBefore());
        }
        if (entity.getBgAfter() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getBgAfter());
        }
        if (entity.getBpSys() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getBpSys());
        }
        if (entity.getBpDia() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getBpDia());
        }
        if (entity.getHeartRate() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getHeartRate());
        }
        if (entity.getNote() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getNote());
        }
        statement.bindLong(14, entity.getId());
      }
    });
  }

  @Override
  public Object insert(final LogEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLogEntry.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final LogEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfLogEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final LogEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfLogEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final LogEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfLogEntry.upsert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LogEntry>> getByDate(final String date) {
    final String _sql = "SELECT * FROM log_entries WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"log_entries"}, new Callable<List<LogEntry>>() {
      @Override
      @NonNull
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSession = CursorUtil.getColumnIndexOrThrow(_cursor, "session");
          final int _cursorIndexOfMedType = CursorUtil.getColumnIndexOrThrow(_cursor, "medType");
          final int _cursorIndexOfDose = CursorUtil.getColumnIndexOrThrow(_cursor, "dose");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfBgBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "bgBefore");
          final int _cursorIndexOfBgAfter = CursorUtil.getColumnIndexOrThrow(_cursor, "bgAfter");
          final int _cursorIndexOfBpSys = CursorUtil.getColumnIndexOrThrow(_cursor, "bpSys");
          final int _cursorIndexOfBpDia = CursorUtil.getColumnIndexOrThrow(_cursor, "bpDia");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final String _tmpSession;
            if (_cursor.isNull(_cursorIndexOfSession)) {
              _tmpSession = null;
            } else {
              _tmpSession = _cursor.getString(_cursorIndexOfSession);
            }
            final String _tmpMedType;
            if (_cursor.isNull(_cursorIndexOfMedType)) {
              _tmpMedType = null;
            } else {
              _tmpMedType = _cursor.getString(_cursorIndexOfMedType);
            }
            final String _tmpDose;
            if (_cursor.isNull(_cursorIndexOfDose)) {
              _tmpDose = null;
            } else {
              _tmpDose = _cursor.getString(_cursorIndexOfDose);
            }
            final String _tmpTime;
            if (_cursor.isNull(_cursorIndexOfTime)) {
              _tmpTime = null;
            } else {
              _tmpTime = _cursor.getString(_cursorIndexOfTime);
            }
            final int _tmpValue;
            _tmpValue = _cursor.getInt(_cursorIndexOfValue);
            final Double _tmpBgBefore;
            if (_cursor.isNull(_cursorIndexOfBgBefore)) {
              _tmpBgBefore = null;
            } else {
              _tmpBgBefore = _cursor.getDouble(_cursorIndexOfBgBefore);
            }
            final Double _tmpBgAfter;
            if (_cursor.isNull(_cursorIndexOfBgAfter)) {
              _tmpBgAfter = null;
            } else {
              _tmpBgAfter = _cursor.getDouble(_cursorIndexOfBgAfter);
            }
            final Integer _tmpBpSys;
            if (_cursor.isNull(_cursorIndexOfBpSys)) {
              _tmpBpSys = null;
            } else {
              _tmpBpSys = _cursor.getInt(_cursorIndexOfBpSys);
            }
            final Integer _tmpBpDia;
            if (_cursor.isNull(_cursorIndexOfBpDia)) {
              _tmpBpDia = null;
            } else {
              _tmpBpDia = _cursor.getInt(_cursorIndexOfBpDia);
            }
            final Integer _tmpHeartRate;
            if (_cursor.isNull(_cursorIndexOfHeartRate)) {
              _tmpHeartRate = null;
            } else {
              _tmpHeartRate = _cursor.getInt(_cursorIndexOfHeartRate);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new LogEntry(_tmpId,_tmpDate,_tmpSession,_tmpMedType,_tmpDose,_tmpTime,_tmpValue,_tmpBgBefore,_tmpBgAfter,_tmpBpSys,_tmpBpDia,_tmpHeartRate,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getTotalCount() {
    final String _sql = "SELECT COUNT(*) FROM log_entries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"log_entries"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LogEntry>> getEntriesForDate(final String date) {
    final String _sql = "SELECT * FROM log_entries WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"log_entries"}, new Callable<List<LogEntry>>() {
      @Override
      @NonNull
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSession = CursorUtil.getColumnIndexOrThrow(_cursor, "session");
          final int _cursorIndexOfMedType = CursorUtil.getColumnIndexOrThrow(_cursor, "medType");
          final int _cursorIndexOfDose = CursorUtil.getColumnIndexOrThrow(_cursor, "dose");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfBgBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "bgBefore");
          final int _cursorIndexOfBgAfter = CursorUtil.getColumnIndexOrThrow(_cursor, "bgAfter");
          final int _cursorIndexOfBpSys = CursorUtil.getColumnIndexOrThrow(_cursor, "bpSys");
          final int _cursorIndexOfBpDia = CursorUtil.getColumnIndexOrThrow(_cursor, "bpDia");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final String _tmpSession;
            if (_cursor.isNull(_cursorIndexOfSession)) {
              _tmpSession = null;
            } else {
              _tmpSession = _cursor.getString(_cursorIndexOfSession);
            }
            final String _tmpMedType;
            if (_cursor.isNull(_cursorIndexOfMedType)) {
              _tmpMedType = null;
            } else {
              _tmpMedType = _cursor.getString(_cursorIndexOfMedType);
            }
            final String _tmpDose;
            if (_cursor.isNull(_cursorIndexOfDose)) {
              _tmpDose = null;
            } else {
              _tmpDose = _cursor.getString(_cursorIndexOfDose);
            }
            final String _tmpTime;
            if (_cursor.isNull(_cursorIndexOfTime)) {
              _tmpTime = null;
            } else {
              _tmpTime = _cursor.getString(_cursorIndexOfTime);
            }
            final int _tmpValue;
            _tmpValue = _cursor.getInt(_cursorIndexOfValue);
            final Double _tmpBgBefore;
            if (_cursor.isNull(_cursorIndexOfBgBefore)) {
              _tmpBgBefore = null;
            } else {
              _tmpBgBefore = _cursor.getDouble(_cursorIndexOfBgBefore);
            }
            final Double _tmpBgAfter;
            if (_cursor.isNull(_cursorIndexOfBgAfter)) {
              _tmpBgAfter = null;
            } else {
              _tmpBgAfter = _cursor.getDouble(_cursorIndexOfBgAfter);
            }
            final Integer _tmpBpSys;
            if (_cursor.isNull(_cursorIndexOfBpSys)) {
              _tmpBpSys = null;
            } else {
              _tmpBpSys = _cursor.getInt(_cursorIndexOfBpSys);
            }
            final Integer _tmpBpDia;
            if (_cursor.isNull(_cursorIndexOfBpDia)) {
              _tmpBpDia = null;
            } else {
              _tmpBpDia = _cursor.getInt(_cursorIndexOfBpDia);
            }
            final Integer _tmpHeartRate;
            if (_cursor.isNull(_cursorIndexOfHeartRate)) {
              _tmpHeartRate = null;
            } else {
              _tmpHeartRate = _cursor.getInt(_cursorIndexOfHeartRate);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new LogEntry(_tmpId,_tmpDate,_tmpSession,_tmpMedType,_tmpDose,_tmpTime,_tmpValue,_tmpBgBefore,_tmpBgAfter,_tmpBpSys,_tmpBpDia,_tmpHeartRate,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LogEntry>> getAllLogEntries() {
    final String _sql = "SELECT * FROM log_entries ORDER BY date DESC, time DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"log_entries"}, new Callable<List<LogEntry>>() {
      @Override
      @NonNull
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSession = CursorUtil.getColumnIndexOrThrow(_cursor, "session");
          final int _cursorIndexOfMedType = CursorUtil.getColumnIndexOrThrow(_cursor, "medType");
          final int _cursorIndexOfDose = CursorUtil.getColumnIndexOrThrow(_cursor, "dose");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfBgBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "bgBefore");
          final int _cursorIndexOfBgAfter = CursorUtil.getColumnIndexOrThrow(_cursor, "bgAfter");
          final int _cursorIndexOfBpSys = CursorUtil.getColumnIndexOrThrow(_cursor, "bpSys");
          final int _cursorIndexOfBpDia = CursorUtil.getColumnIndexOrThrow(_cursor, "bpDia");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final String _tmpSession;
            if (_cursor.isNull(_cursorIndexOfSession)) {
              _tmpSession = null;
            } else {
              _tmpSession = _cursor.getString(_cursorIndexOfSession);
            }
            final String _tmpMedType;
            if (_cursor.isNull(_cursorIndexOfMedType)) {
              _tmpMedType = null;
            } else {
              _tmpMedType = _cursor.getString(_cursorIndexOfMedType);
            }
            final String _tmpDose;
            if (_cursor.isNull(_cursorIndexOfDose)) {
              _tmpDose = null;
            } else {
              _tmpDose = _cursor.getString(_cursorIndexOfDose);
            }
            final String _tmpTime;
            if (_cursor.isNull(_cursorIndexOfTime)) {
              _tmpTime = null;
            } else {
              _tmpTime = _cursor.getString(_cursorIndexOfTime);
            }
            final int _tmpValue;
            _tmpValue = _cursor.getInt(_cursorIndexOfValue);
            final Double _tmpBgBefore;
            if (_cursor.isNull(_cursorIndexOfBgBefore)) {
              _tmpBgBefore = null;
            } else {
              _tmpBgBefore = _cursor.getDouble(_cursorIndexOfBgBefore);
            }
            final Double _tmpBgAfter;
            if (_cursor.isNull(_cursorIndexOfBgAfter)) {
              _tmpBgAfter = null;
            } else {
              _tmpBgAfter = _cursor.getDouble(_cursorIndexOfBgAfter);
            }
            final Integer _tmpBpSys;
            if (_cursor.isNull(_cursorIndexOfBpSys)) {
              _tmpBpSys = null;
            } else {
              _tmpBpSys = _cursor.getInt(_cursorIndexOfBpSys);
            }
            final Integer _tmpBpDia;
            if (_cursor.isNull(_cursorIndexOfBpDia)) {
              _tmpBpDia = null;
            } else {
              _tmpBpDia = _cursor.getInt(_cursorIndexOfBpDia);
            }
            final Integer _tmpHeartRate;
            if (_cursor.isNull(_cursorIndexOfHeartRate)) {
              _tmpHeartRate = null;
            } else {
              _tmpHeartRate = _cursor.getInt(_cursorIndexOfHeartRate);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new LogEntry(_tmpId,_tmpDate,_tmpSession,_tmpMedType,_tmpDose,_tmpTime,_tmpValue,_tmpBgBefore,_tmpBgAfter,_tmpBpSys,_tmpBpDia,_tmpHeartRate,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<String>> getAllDates() {
    final String _sql = "SELECT DISTINCT date FROM log_entries ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"log_entries"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            if (_cursor.isNull(0)) {
              _item = null;
            } else {
              _item = _cursor.getString(0);
            }
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<AdvancedStatsEntity> getAdvancedStats() {
    final String _sql = "SELECT COUNT(*) as totalValid FROM log_entries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"log_entries"}, new Callable<AdvancedStatsEntity>() {
      @Override
      @NonNull
      public AdvancedStatsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTotalValid = 0;
          final AdvancedStatsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpTotalValid;
            _tmpTotalValid = _cursor.getInt(_cursorIndexOfTotalValid);
            _result = new AdvancedStatsEntity(_tmpTotalValid);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<DailyAvgRow>> getDailyAverage() {
    final String _sql = "SELECT date, AVG(bgBefore) as averageValue FROM log_entries GROUP BY date";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"log_entries"}, new Callable<List<DailyAvgRow>>() {
      @Override
      @NonNull
      public List<DailyAvgRow> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = 0;
          final int _cursorIndexOfAverageValue = 1;
          final List<DailyAvgRow> _result = new ArrayList<DailyAvgRow>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyAvgRow _item;
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final double _tmpAverageValue;
            _tmpAverageValue = _cursor.getDouble(_cursorIndexOfAverageValue);
            _item = new DailyAvgRow(_tmpDate,_tmpAverageValue);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
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
}
