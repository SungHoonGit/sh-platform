package com.shplatform.auth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * 모든 리포지토리의 파생 쿼리가 기동 시점에 생성 가능한지 검증하는 스모크 테스트.
 *
 * <p>Spring Data JPA는 애플리케이션 시작 중에 메서드명 기반 쿼리를 생성하므로,
 * 잘못된 시그니처(파라미터 수 불일치 등)는 단위 테스트(Mockito)로는 잡히지 않고
 * 프로덕션 기동 실패로 이어진다 (docs/errors/010 참조).
 * 본 테스트는 H2 위에서 JPA 슬라이스를 띄워 그 실패를 CI 단계에서 잡는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RepositoriesSmokeTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void allRepositoryQueries_shouldBeCreatable() {
        assertNotNull(entityManager);
    }
}
