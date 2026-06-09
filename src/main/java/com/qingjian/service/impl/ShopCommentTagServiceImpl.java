package com.qingjian.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingjian.dto.Result;
import com.qingjian.entity.ShopCommentTag;
import com.qingjian.mapper.ShopCommentTagMapper;
import com.qingjian.service.IShopCommentTagService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 评价标签统计 服务实现类
 * </p>
 */
@Service
public class ShopCommentTagServiceImpl extends ServiceImpl<ShopCommentTagMapper, ShopCommentTag> implements IShopCommentTagService {

    @Override
    public Result queryShopCommentTags(Long shopId) {
        List<ShopCommentTag> tags = lambdaQuery()
                .eq(ShopCommentTag::getShopId, shopId)
                .orderByDesc(ShopCommentTag::getTagCount)
                .list();
        return Result.ok(tags);
    }
}
