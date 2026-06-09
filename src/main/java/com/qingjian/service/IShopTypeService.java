package com.qingjian.service;

import com.qingjian.dto.Result;
import com.qingjian.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopTypeService extends IService<ShopType> {
    Result queryshopTypeList();
}
