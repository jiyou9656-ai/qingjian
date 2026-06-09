package com.qingjian.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingjian.dto.Result;
import com.qingjian.dto.UserDTO;
import com.qingjian.entity.BlogComments;
import com.qingjian.entity.User;
import com.qingjian.mapper.BlogCommentsMapper;
import com.qingjian.service.IBlogCommentsService;
import com.qingjian.service.IUserService;
import com.qingjian.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

    @Resource
    private IUserService userService;

    @Override
    public Result queryBlogComments(Long blogId) {
        // 查询一级评论
        List<BlogComments> comments = this.lambdaQuery()
                .eq(BlogComments::getBlogId, blogId)
                .eq(BlogComments::getParentId, 0)
                .orderByDesc(BlogComments::getCreateTime)
                .last("LIMIT 50")
                .list();

        if (comments == null || comments.isEmpty()) {
            return Result.ok(java.util.Collections.emptyList());
        }

        // 查询评论对应的用户信息
        List<Long> userIds = comments.stream()
                .map(BlogComments::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userService.listByIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 组装返回数据
        List<Map<String, Object>> result = comments.stream().map(c -> {
            Map<String, Object> map = BeanUtil.beanToMap(c);
            User user = userMap.get(c.getUserId());
            if (user != null) {
                map.put("icon", user.getIcon());
                map.put("nickName", user.getNickName());
            }
            return map;
        }).collect(Collectors.toList());

        return Result.ok(result);
    }

    @Override
    @Transactional
    public Result saveBlogComment(BlogComments comment) {
        Long userId = UserHolder.getUser().getId();
        comment.setUserId(userId);
        comment.setParentId(0L);      // 一级评论
        comment.setAnswerId(0L);      // 一级评论没有回复目标
        comment.setLiked(0);
        comment.setStatus(false);     // 0：正常
        save(comment);
        return Result.ok();
    }

    @Override
    @Transactional
    public Result likeBlogComment(Long commentId) {
        // 获取当前用户
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();

        // 判断是否已经点过赞（简单实现：每次点赞 +1，没有做去重表）
        update().setSql("liked = liked + 1")
                .eq("id", commentId)
                .update();

        return Result.ok();
    }
}