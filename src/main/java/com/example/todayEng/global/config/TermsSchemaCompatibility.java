package com.example.todayEng.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;

@Component
@RequiredArgsConstructor
public class TermsSchemaCompatibility {

    private final JdbcTemplate jdbcTemplate;

    public void prepare() {
        if (hasIndex("uk_terms_type")) {
            jdbcTemplate.execute("ALTER TABLE terms DROP INDEX uk_terms_type");
        }
    }

    private boolean hasIndex(String indexName) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (ResultSet indexes = connection.getMetaData()
                    .getIndexInfo(connection.getCatalog(), null, "terms", true, false)) {
                while (indexes.next()) {
                    if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                        return true;
                    }
                }
                return false;
            }
        }));
    }
}
