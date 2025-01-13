CREATE TABLE IF NOT EXISTS file_metadata
(
    id            UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    relative_path VARCHAR(255),
    last_modified TIMESTAMP
);

CREATE UNIQUE INDEX relative_path_unique_idx ON file_metadata (relative_path);