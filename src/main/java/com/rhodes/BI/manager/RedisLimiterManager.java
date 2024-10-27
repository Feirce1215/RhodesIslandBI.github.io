package com.rhodes.BI.manager;

import com.rhodes.BI.common.ErrorCode;
import com.rhodes.BI.exception.BusinessException;
import org.redisson.api.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 限流基础服务
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     *
     * @param key 用于区分不同的限流器
     */
    public void doRateLimiter(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, 1, 1, RateIntervalUnit.SECONDS);

        boolean acquire = rateLimiter.tryAcquire(1);
        if (!acquire) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
