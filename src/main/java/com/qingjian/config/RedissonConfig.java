package com.qingjian.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        log.info("开始创建 RedissonClient...");
        Config config = new Config();
        String address = "redis://127.0.0.1:6379";
        String password = "123456";
        
        log.info("Redis 地址: {}", address);
        log.info("Redis 密码: {}", password);
        
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setPingConnectionInterval(0);
        
        try {
            RedissonClient client = Redisson.create(config);
            log.info("RedissonClient 创建成功！");
            return client;
        } catch (Exception e) {
            log.error("RedissonClient 创建失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
