package com.qingjian.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingjian.dto.Result;
import com.qingjian.dto.UserDTO;
import com.qingjian.entity.Shop;
import com.qingjian.entity.ShopComment;
import com.qingjian.entity.ShopCommentTag;
import com.qingjian.entity.User;
import com.qingjian.mapper.ShopCommentMapper;
import com.qingjian.service.IShopCommentService;
import com.qingjian.service.IShopCommentTagService;
import com.qingjian.service.IShopService;
import com.qingjian.service.IUserService;
import com.qingjian.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 商户评价 服务实现类
 * </p>
 */
@Service
public class ShopCommentServiceImpl extends ServiceImpl<ShopCommentMapper, ShopComment> implements IShopCommentService {

    @Resource
    private IUserService userService;

    @Resource
    private IShopService shopService;

    @Resource
    private IShopCommentTagService shopCommentTagService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopComments(Long shopId, Integer current, Integer size) {
        // 1. 分页查询评价
        Page<ShopComment> page = new Page<>(current, size);
        page = lambdaQuery()
                .eq(ShopComment::getShopId, shopId)
                .orderByDesc(ShopComment::getCreateTime)
                .page(page);

        List<ShopComment> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            Map<String, Object> pageResult = new java.util.HashMap<>();
            pageResult.put("records", Collections.emptyList());
            pageResult.put("total", page.getTotal());
            return Result.ok(pageResult);
        }

        // 2. 查询用户信息
        List<Long> userIds = records.stream()
                .map(ShopComment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 3. 组装返回数据
        List<Map<String, Object>> result = records.stream().map(c -> {
            Map<String, Object> map = BeanUtil.beanToMap(c);
            User user = userMap.get(c.getUserId());
            if (user != null) {
                map.put("userName", user.getNickName());
                map.put("userIcon", user.getIcon());
            }
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> pageResult = new java.util.HashMap<>();
        pageResult.put("records", result);
        pageResult.put("total", page.getTotal());
        return Result.ok(pageResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveComment(Long shopId, Integer score, String content, String tags) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }

        // 校验输入参数
        if (score == null || score < 1 || score > 5) {
            return Result.fail("评分必须在1-5分之间");
        }
        if (content == null || content.trim().isEmpty()) {
            return Result.fail("评价内容不能为空");
        }

        // 校验商户是否存在
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            return Result.fail("商户不存在");
        }

        // 1. 创建评价
        ShopComment comment = new ShopComment();
        comment.setShopId(shopId);
        comment.setUserId(user.getId());
        comment.setScore(score);
        comment.setContent(content.trim());
        comment.setBrowseCount(0);
        save(comment);

        // 2. 更新评价标签统计
        if (tags != null && !tags.isEmpty()) {
            String[] tagArr = tags.split(",");
            for (String tag : tagArr) {
                String tagName = tag.trim();
                if (tagName.isEmpty()) continue;

                // 查询是否已有该标签
                ShopCommentTag existTag = shopCommentTagService.lambdaQuery()
                        .eq(ShopCommentTag::getShopId, shopId)
                        .eq(ShopCommentTag::getTagName, tagName)
                        .one();

                if (existTag != null) {
                    // 已存在，计数 +1
                    existTag.setTagCount(existTag.getTagCount() + 1);
                    shopCommentTagService.updateById(existTag);
                } else {
                    // 不存在，新建
                    ShopCommentTag newTag = new ShopCommentTag();
                    newTag.setShopId(shopId);
                    newTag.setTagName(tagName);
                    newTag.setTagCount(1);
                    shopCommentTagService.save(newTag);
                }
            }
        }

        // 3. 更新商户评论数
        shopService.lambdaUpdate()
                .eq(Shop::getId, shopId)
                .setSql("comments = comments + 1")
                .update();

        return Result.ok(comment.getId());
    }

    private static final String LIKE_LUA_SCRIPT =
            "if redis.call('sismember', KEYS[1], ARGV[1]) == 1 then " +
            "    return 0 " +
            "end " +
            "redis.call('sadd', KEYS[1], ARGV[1]) " +
            "return 1";

    @Override
    public Result likeComment(Long commentId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }

        String key = "shop:comment:like:" + commentId;
        String userId = user.getId().toString();

        // 使用Lua脚本保证判断+添加的原子性，防止并发重复点赞
        Long result = stringRedisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(LIKE_LUA_SCRIPT, Long.class),
                Collections.singletonList(key),
                userId
        );

        if (result == null || result == 0) {
            return Result.fail("已经点过赞了");
        }

        // 原子操作成功，更新数据库点赞数
        lambdaUpdate()
                .eq(ShopComment::getId, commentId)
                .setSql("like_count = like_count + 1")
                .update();

        return Result.ok();
    }
}
