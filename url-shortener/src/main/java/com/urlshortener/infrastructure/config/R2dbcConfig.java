package com.urlshortener.infrastructure.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * R2DBC configuration.
 *
 * <p>The {@link ConnectionFactory} is auto-configured by Spring Boot from
 * {@code spring.r2dbc.*} application properties. This class only registers
 * the {@link DatabaseClient} and transaction manager beans.
 */
@Configuration
public class R2dbcConfig {

    @Bean
    public DatabaseClient databaseClient(ConnectionFactory connectionFactory) {
        return DatabaseClient.create(connectionFactory);
    }

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}
