CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    role       ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE datasets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    s3_key      VARCHAR(512) NOT NULL,
    row_count   INT,
    col_count   INT,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE eval_jobs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id    BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    status        ENUM('PENDING','EVALUATING','SCORING','GENERATING_REPORT','DONE','FAILED') DEFAULT 'PENDING',
    progress      INT DEFAULT 0,
    error_msg     TEXT,
    result_s3_key VARCHAR(512),
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (dataset_id) REFERENCES datasets(id)
);

CREATE TABLE eval_results (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id        BIGINT NOT NULL,
    criteria_name VARCHAR(100) NOT NULL,
    score         DECIMAL(5,2) NOT NULL,
    weight        DECIMAL(4,3) NOT NULL,
    detail        TEXT,
    FOREIGN KEY (job_id) REFERENCES eval_jobs(id)
);
