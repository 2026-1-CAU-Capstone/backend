package com.jazzify.backend.core.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.DispatcherType;

@NullMarked
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@Test
	void asyncDispatcherIsNotRejectedByAuthentication() throws Exception {
		mockMvc.perform(get("/secured-missing-path")
				.with(request -> {
					request.setDispatcherType(DispatcherType.ASYNC);
					return request;
				}))
			.andExpect(status().isInternalServerError());
	}

	@Test
	void errorDispatcherIsNotRejectedByAuthentication() throws Exception {
		mockMvc.perform(get("/secured-missing-path")
				.with(request -> {
					request.setDispatcherType(DispatcherType.ERROR);
					return request;
				}))
			.andExpect(status().isInternalServerError());
	}
}
