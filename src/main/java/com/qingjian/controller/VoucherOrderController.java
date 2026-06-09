package com.qingjian.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.qingjian.dto.Result;
import com.qingjian.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        log.info("秒杀券ID: {}", voucherId);
        Result result = voucherOrderService.seckillVoucher(voucherId);
        return result;
    }

    @PostMapping("pay/{id}")
    public Result pay(@PathVariable("id") Long orderId) {
        return voucherOrderService.pay(orderId);
    }

    @GetMapping("pay/status/{id}")
    public Result queryPayStatus(@PathVariable("id") Long orderId) {
        return voucherOrderService.queryPayStatus(orderId);
    }

    @GetMapping("list")
    public Result myOrders(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size) {
        return voucherOrderService.myOrders(current, size);
    }

}
