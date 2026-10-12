package com.example.quanlydetai.exception;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaSystemException;

import java.sql.SQLException;

class DatabaseErrorMessagesTest {
    @Test
    void unwrapsOnlyBusinessSqlstateThroughCauseAndNextException() {
        var outer = new SQLException("driver context");
        outer.setNextException(
                new SQLException("Nhóm không được vượt quá 3 sinh viên.", "45000", 1644));
        assertThat(
                        DatabaseErrorMessages.extractRootCauseMessage(
                                new JpaSystemException(new RuntimeException(outer))))
                .isEqualTo("Nhóm không được vượt quá 3 sinh viên.");
    }

    @Test
    void hidesRawSqlAndDuplicateKeyDetails() {
        assertThat(
                        DatabaseErrorMessages.extractRootCauseMessage(
                                new SQLException("secret SQL password", "23000")))
                .doesNotContain("secret", "password", "SQL");
    }

    @Test
    void cyclicCausesCannotHangHandler() {
        var one = new RuntimeException();
        var two = new RuntimeException(one);
        one.initCause(two);
        assertThat(DatabaseErrorMessages.extractRootCauseMessage(one)).isNotBlank();
    }
}
