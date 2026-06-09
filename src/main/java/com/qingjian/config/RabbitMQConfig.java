package com.qingjian.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

    // 死信交换机与队列
    public static final String SECKILL_DLX_EXCHANGE = "seckill.dlx.exchange";
    public static final String SECKILL_DLX_QUEUE = "seckill.dlx.queue";
    public static final String SECKILL_DLX_ROUTING_KEY = "seckill.dlx.order";

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillQueue() {
        Map<String, Object> args = new HashMap<>();
        // 消息过期时间：10分钟
        args.put("x-message-ttl", 600000);
        // 死信交换机
        args.put("x-dead-letter-exchange", SECKILL_DLX_EXCHANGE);
        // 死信路由键
        args.put("x-dead-letter-routing-key", SECKILL_DLX_ROUTING_KEY);
        return new Queue(SECKILL_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(SECKILL_ROUTING_KEY);
    }

    // ===== 死信队列配置 =====

    @Bean
    public DirectExchange seckillDlxExchange() {
        return new DirectExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillDlxQueue() {
        return new Queue(SECKILL_DLX_QUEUE, true);
    }

    @Bean
    public Binding seckillDlxBinding() {
        return BindingBuilder.bind(seckillDlxQueue())
                .to(seckillDlxExchange())
                .with(SECKILL_DLX_ROUTING_KEY);
    }
}
