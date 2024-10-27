package com.rhodes.BI.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;

@SpringBootTest
class RedisLimiterManagerTest {

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Test
    public void doRateLimiter() {
        String userId = "1";
        for (int i = 0; i < 5; i ++) {
            redisLimiterManager.doRateLimiter(userId);
            System.out.println("success");
        }
    }
}