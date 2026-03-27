package com.jazzify.backend.core.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NullMarked
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	private String secret;
	private long accessExpiration;
	private long refreshExpiration;
}
