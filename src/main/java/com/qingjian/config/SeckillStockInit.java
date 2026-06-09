package com.qingjian.config;

import com.qingjian.entity.SeckillVoucher;
import com.qingjian.service.ISeckillVoucherService;
import com.qingjian.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class SeckillStockInit implements CommandLineRunner {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(String... args) {
        List<SeckillVoucher> list = seckillVoucherService.list();
        if (list == null || list.isEmpty()) {
            log.info("暂无秒杀券数据");
            return;
        }
        for (SeckillVoucher sv : list) {
            String key = RedisConstants.SECKILL_STOCK_KEY + sv.getVoucherId();
            stringRedisTemplate.opsForValue().set(key, sv.getStock().toString());
            log.info("初始化秒杀库存: voucherId={}, stock={}", sv.getVoucherId(), sv.getStock());
        }
        log.info("秒杀库存初始化完成，共 {} 个券", list.size());
    }
}