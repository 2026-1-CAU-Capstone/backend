package com.jazzify.backend.core.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;

/**
 * 메인 MySQL DataSource 명시적 설정.
 *
 * <p>RAG용 PostgreSQL DataSource({@code ragDataSource})가 컨텍스트에 등록되면
 * Spring Boot의 {@code DataSourceAutoConfiguration}이
 * {@code @ConditionalOnMissingBean(DataSource.class)} 조건으로 인해 백오프(back-off)된다.
 * 그 결과 Hibernate JPA가 유일하게 남은 PostgreSQL DataSource를 사용하려 시도한다.
 *
 * <p>이를 방지하기 위해 {@code spring.datasource.*} 프로퍼티를 읽어 MySQL DataSource를
 * {@code @Primary}로 명시적으로 등록한다.
 *
 * <p>DataSource가 두 개 이상이 되면 Spring Boot의
 * {@code DataSourceTransactionManagerAutoConfiguration}도 백오프하여
 * 기본 {@code transactionManager} 빈이 생성되지 않는다.
 * {@code @Transactional} (qualifier 없음)이 사용하는 기본 트랜잭션 매니저를
 * 명시적으로 {@code @Primary}로 등록하여 이를 해결한다.
 */
@NullMarked
@Configuration
public class MainDataSourceConfig {

	@Value("${spring.datasource.url}")
	private String url;

	@Value("${spring.datasource.username}")
	private String username;

	@Value("${spring.datasource.password:}")
	private String password;

	@Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
	private String driverClassName;

	@Value("${spring.datasource.hikari.maximum-pool-size:10}")
	private int maximumPoolSize;

	@Value("${spring.datasource.hikari.minimum-idle:2}")
	private int minimumIdle;

	@Primary
	@Bean(name = "dataSource")
	public HikariDataSource dataSource() {
		HikariDataSource ds = DataSourceBuilder.create()
			.type(HikariDataSource.class)
			.driverClassName(driverClassName)
			.url(url)
			.username(username)
			.password(password)
			.build();
		ds.setMaximumPoolSize(maximumPoolSize);
		ds.setMinimumIdle(minimumIdle);
		ds.setPoolName("main-mysql-pool");
		return ds;
	}

	/**
	 * MySQL 기반 기본 트랜잭션 매니저.
	 *
	 * <p>DataSource가 두 개 이상일 때 {@code DataSourceTransactionManagerAutoConfiguration}이
	 * 백오프하므로 명시적으로 {@code @Primary}로 등록하여
	 * {@code @Transactional} (qualifier 없음)의 기본 대상이 되도록 한다.
	 * JPA Repository를 사용하므로 {@link JpaTransactionManager}를 사용한다.
	 */
	@Primary
	@Bean(name = "transactionManager")
	public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}
