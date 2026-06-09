package com.qingjian.service;

import com.qingjian.dto.Result;
import com.qingjian.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result pay(Long orderId);

    Result queryPayStatus(Long orderId);

    Result myOrders(Integer current, Integer size);

    Result seckillVoucher(Long voucherId);

   void createVoucherOrder(VoucherOrder voucherOrder);
}
