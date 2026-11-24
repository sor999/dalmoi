package com.sor999.dalmoi;

import com.sor999.dalmoi.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresIntegrationTest
class DalmoiApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("애플리케이션이 독립된 PostgreSQL 테스트 DB와 JPA를 초기화한다")
    void initializesWithPostgresTestDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5)).isTrue();
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.getCatalog()).isEqualTo("dalmoi_test");
        }
        assertThat(entityManagerFactory.isOpen()).isTrue();
    }

}
