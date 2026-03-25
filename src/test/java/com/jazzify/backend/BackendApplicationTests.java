package com.jazzify.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BackendApplicationTests {

	@MockitoBean
	StringRedisTemplate stringRedisTemplate;

	@Test
	void contextLoads() {
	}

}
