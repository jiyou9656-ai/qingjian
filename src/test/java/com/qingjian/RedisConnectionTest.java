package com.qingjian;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RedisConnectionTest {

    @Test
    void testRedisConnection() {
        System.out.println("开始测试 Redis 连接...");
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setPassword("123456")
                .setDatabase(0)
                .setConnectionMinimumIdleSize(10)
                .setConnectionPoolSize(64)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        try {
            RedissonClient client = Redisson.create(config);
            System.out.println("✅ Redis 连接成功！");
            client.shutdown();
        } catch (Exception e) {
            System.out.println("❌ Redis 连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
