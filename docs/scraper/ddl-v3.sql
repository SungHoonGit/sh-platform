-- scraper DDL v3: 공고 스크랩(북마크) 기능
-- 사용자별 공고 저장. resume 모듈의 지원관리에서 posting_id로 참조한다.

CREATE TABLE IF NOT EXISTS job_scraps (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    posting_id  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_scraps_user_posting UNIQUE (user_id, posting_id),
    INDEX idx_job_scraps_user (user_id, created_at),
    INDEX idx_job_scraps_posting (posting_id)
);
