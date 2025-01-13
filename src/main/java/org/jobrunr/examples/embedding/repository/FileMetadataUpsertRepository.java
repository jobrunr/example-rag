package org.jobrunr.examples.embedding.repository;

import org.jobrunr.examples.embedding.model.FileMetadata;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;

public interface FileMetadataUpsertRepository {

    void upsertAll(List<FileMetadata> entities);

    class FileMetadataUpsertRepositoryImpl implements FileMetadataUpsertRepository {

        private final JdbcTemplate jdbcTemplate;

        public FileMetadataUpsertRepositoryImpl(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }


        @Override
        public void upsertAll(List<FileMetadata> entities) {
            if (entities.isEmpty()) return;

            String sql = """
                    INSERT INTO file_metadata (relative_path, last_modified)
                    VALUES(?, ?) ON CONFLICT (relative_path)
                    DO UPDATE SET last_modified = EXCLUDED.last_modified;
                    """;

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {

                    FileMetadata fileMetadata = entities.get(i);
                    ps.setString(1, fileMetadata.getRelativePath());
                    ps.setObject(2, fileMetadata.getLastModified().atOffset(ZoneOffset.UTC));
                }

                @Override
                public int getBatchSize() {
                    return entities.size();
                }
            });
        }
    }
}
