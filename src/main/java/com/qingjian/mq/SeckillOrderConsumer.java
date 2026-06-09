package com.qingjian.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.qingjian.config.RabbitMQConfig;
import com.qingjian.entity.VoucherOrder;
import com.qingjian.service.IVoucherOrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SeckillOrderConsumer {

    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String SECKILL_ORDER_IDEMPOTENT_KEY = "seckill:order:idempotent:";

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void receiveSeckillOrder(String msg, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("消费者收到消息: {}", msg);

        VoucherOrder voucherOrder = null;
        try {
            // 直接反序列化为目标对象
            voucherOrder = JSON.parseObject(msg, VoucherOrder.class);
            log.info("解析后的订单对象: {}", voucherOrder);
        } catch (JSONException e) {
            log.error("消息JSON解析失败，丢弃消息: {}", msg, e);
            // 不可重试：直接拒绝，不重新入队（进入死信队列）
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        // 幂等校验：基于订单ID防止重复消费
        String idempotentKey = SECKILL_ORDER_IDEMPOTENT_KEY + voucherOrder.getId();
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 30, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("订单已处理过，幂等跳过: orderId={}", voucherOrder.getId());
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            voucherOrderService.createVoucherOrder(voucherOrder);
            channel.basicAck(deliveryTag, false);
            log.info("订单创建成功: {}", voucherOrder.getId());
        } catch (Exception e) {
            log.error("消费订单失败: {}", msg, e);

            // 删除幂等键，允许重试
            stringRedisTemplate.delete(idempotentKey);

            // 判断是否为可重试异常
            if (isRetryableException(e)) {
                // 可重试：重新入队（限5次）
                long retryCount = getRetryCount(message);
                if (retryCount < 5) {
                    log.warn("可重试异常，第{}次重试: {}", retryCount + 1, e.getMessage());
                    channel.basicNack(deliveryTag, false, true);
                } else {
                    log.error("重试次数耗尽，转入死信队列: {}", voucherOrder.getId());
                    channel.basicNack(deliveryTag, false, false);
                }
            } else {
                // 不可重试：直接丢弃（进入死信队列）
                log.error("不可重试异常，丢弃消息: {}", e.getMessage());
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    /**
     * 判断异常是否可重试
     */
    private boolean isRetryableException(Exception e) {
        // 数据库连接超时、网络异常等可重试
        String name = e.getClass().getName();
        return name.contains("Timeout") || name.contains("Connection")
                || name.contains("Network") || name.contains("Socket")
                || name.contains("DataAccess");
    }

    /**
     * 获取当前消息重试次数（基于消息ID的Redis计数）
     */
    private long getRetryCount(Message message) {
        String msgId = message.getMessageProperties().getMessageId();
        if (msgId == null) {
            return 0;
        }
        String key = "seckill:retry:" + msgId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, 10, TimeUnit.MINUTES);
        return count != null ? count - 1 : 0;
    }
}
