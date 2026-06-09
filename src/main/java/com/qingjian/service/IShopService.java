package com.qingjian.service;

import com.qingjian.dto.Result;
import com.qingjian.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y, String sortBy);

    Result queryNearbyShops(Integer current, Double x, Double y, String sortBy);

    Result recommendShops(Double x, Double y);

    Result queryShopRank(Integer current);
}
