package com.qingjian;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.qingjian.mapper")
@SpringBootApplication
public class QingJianApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingJianApplication.class, args);
    }

}
