package com.example.quanlydetai.exception;

import java.sql.SQLException;
import java.util.*;

public final class DatabaseErrorMessages {
    private DatabaseErrorMessages() {}

    public static String extractRootCauseMessage(Throwable exception) {
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        var pending = new ArrayDeque<Throwable>();
        pending.add(exception);
        while (!pending.isEmpty()) {
            var current = pending.removeFirst();
            if (!seen.add(current)) continue;
            // Only our SIGNAL messages are intended for end users, never arbitrary driver SQL text.
            if (current instanceof SQLException sql) {
                if ("45000".equals(sql.getSQLState()) && sql.getMessage() != null)
                    return sql.getMessage();
                if (sql.getNextException() != null) pending.add(sql.getNextException());
            }
            if (current.getCause() != null) pending.add(current.getCause());
        }
        return "Không thể lưu dữ liệu. Dữ liệu có thể đã tồn tại hoặc không còn hợp lệ. Vui lòng"
                + " tải lại và kiểm tra.";
    }
}
