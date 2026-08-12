-- ============================================================
-- [MIGRATION] 2026-08-12: push_subscription 테이블 생성
-- ============================================================
CREATE TABLE IF NOT EXISTS push_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '사용자 ID (auth users.id)',
    endpoint VARCHAR(500) NOT NULL COMMENT '브라우저 푸쉬 엔드포인트',
    p256dh VARCHAR(255) NOT NULL COMMENT '암호화 키 (p256dh)',
    auth_key VARCHAR(255) NOT NULL COMMENT '인증 키 (auth)',
    user_agent VARCHAR(500) COMMENT '브라우저 정보',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_push_subscription_endpoint (endpoint),
    INDEX idx_push_subscription_account (account_id),
    INDEX idx_push_subscription_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='웹 푸쉬 구독 정보';
