package com.jazzify.backend.core.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@NullMarked
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> {
			})
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/v1/auth/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/v1/rag/health").permitAll()
				.requestMatchers(HttpMethod.GET, "/v1/embedding/health").permitAll()
				.requestMatchers(HttpMethod.POST, "/v1/sheet-projects/omr/callback").permitAll()
				.requestMatchers(HttpMethod.POST, "/v1/chord-projects/omr/callback").permitAll()
				.requestMatchers(HttpMethod.POST, "/v1/licks/omr/callback").permitAll()
				.requestMatchers(HttpMethod.POST, "/v1/solos/omr/callback").permitAll()
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				// Embedding 프로브: ADMIN 또는 MANAGE 권한 필요
				.requestMatchers(HttpMethod.POST, "/v1/embedding/**").hasAnyRole("ADMIN", "MANAGE")
				// Lick 쓰기 작업: ADMIN 또는 MANAGE 권한 필요
				.requestMatchers(HttpMethod.POST, "/v1/licks/**").hasAnyRole("ADMIN", "MANAGE")
				.requestMatchers(HttpMethod.PUT, "/v1/licks/**").hasAnyRole("ADMIN", "MANAGE")
				.requestMatchers(HttpMethod.DELETE, "/v1/licks/**").hasAnyRole("ADMIN", "MANAGE")
				// Solo 쓰기 작업: ADMIN 또는 MANAGE 권한 필요
				.requestMatchers(HttpMethod.POST, "/v1/solos/**").hasAnyRole("ADMIN", "MANAGE")
				.requestMatchers(HttpMethod.PUT, "/v1/solos/**").hasAnyRole("ADMIN", "MANAGE")
				.requestMatchers(HttpMethod.DELETE, "/v1/solos/**").hasAnyRole("ADMIN", "MANAGE")
				.anyRequest().authenticated()
			)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
				.accessDeniedHandler(jwtAccessDeniedHandler)
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}
}
