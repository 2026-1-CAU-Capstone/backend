package com.jazzify.backend.domain.rag.config;

import javax.sql.DataSource;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

@NullMarked
@Configuration
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagDataSourceConfig {

	@Bean(name = "ragDataSource")
	public DataSource ragDataSource(RagProperties ragProperties) {
		RagProperties.Datasource datasource = ragProperties.datasource();
		String url = require(datasource.url(), "RAG_DB_URL");
		String username = require(datasource.username(), "RAG_DB_USERNAME");
		String password = require(datasource.password(), "RAG_DB_PASSWORD");

		HikariDataSource dataSource = DataSourceBuilder.create()
			.type(HikariDataSource.class)
			.driverClassName(datasource.driverClassName())
			.url(url)
			.username(username)
			.password(password)
			.build();
		dataSource.setMaximumPoolSize(Math.max(1, datasource.maximumPoolSize()));
		dataSource.setPoolName("rag-pg-pool");
		return dataSource;
	}

	@Bean(name = "ragJdbcTemplate")
	public JdbcTemplate ragJdbcTemplate(DataSource ragDataSource) {
		return new JdbcTemplate(ragDataSource);
	}

	@Bean(name = "ragJdbcClient")
	public JdbcClient ragJdbcClient(JdbcTemplate ragJdbcTemplate) {
		return JdbcClient.create(ragJdbcTemplate);
	}

	@Bean(name = "ragTransactionManager")
	public PlatformTransactionManager ragTransactionManager(DataSource ragDataSource) {
		return new DataSourceTransactionManager(ragDataSource);
	}

	private static String require(@Nullable String value, String envName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(envName + " 환경변수가 필요합니다.");
		}
		return value;
	}
}


