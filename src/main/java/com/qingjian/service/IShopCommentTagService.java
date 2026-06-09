package com.qingjian.service;

import com.qingjian.dto.Result;
import com.qingjian.entity.ShopCommentTag;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 评价标签统计 服务类
 * </p>
 *
 */
public interface IShopCommentTagService extends IService<ShopCommentTag> {

    /**
     * 查询商户评价标签统计
     */
    Result queryShopCommentTags(Long shopId);
}
