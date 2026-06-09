package com.qingjian.service;

import com.qingjian.dto.Result;
import com.qingjian.entity.ShopComment;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商户评价服务类
 * </p>
 *
 */
public interface IShopCommentService extends IService<ShopComment> {

    /**
     * 查询商户评价列表（分页）
     */
    Result queryShopComments(Long shopId, Integer current, Integer size);

    /**
     * 发布评价
     */
    Result saveComment(Long shopId, Integer score, String content, String tags);

    /**
     * 点赞评价
     */
    Result likeComment(Long commentId);
}
