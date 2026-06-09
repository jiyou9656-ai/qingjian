package com.qingjian.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingjian.dto.Result;
import com.qingjian.entity.Notice;
import com.qingjian.entity.User;
import com.qingjian.mapper.NoticeMapper;
import com.qingjian.service.INoticeService;
import com.qingjian.service.IUserService;
import com.qingjian.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper,Notice> implements INoticeService {

    @Resource
    private IUserService userService;

    @Override
    public Result list(Integer current) {
        Long userId = UserHolder.getUser().getId();
        Page<Notice> page = query().eq("user_id", userId)
                .orderByDesc("create_time")
                .page(new Page<>(current, 10));

        List<Notice> notices = page.getRecords();
        if (notices.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 收集所有发送者ID
        List<Long> senderIds = notices.stream()
                .map(Notice::getSenderId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询用户并填充头像和昵称
        if (!senderIds.isEmpty()) {
            Map<Long, User> userMap = userService.listByIds(senderIds)
                    .stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            notices.forEach(notice -> {
                User user = userMap.get(notice.getSenderId());
                if (user != null) {
                    notice.setIcon(user.getIcon());
                    notice.setNickname(user.getNickName());
                }
            });
        }
        return Result.ok(notices);
    }


    @Override
    public Result countUnread() {
        Long userId = UserHolder.getUser().getId();
        Long count = Long.valueOf(query()
                .eq("user_id", userId)
                .eq("is_read", false)
                .count());
        return Result.ok(count);
    }

    @Override
    public Result markRead(Long id) {
        Long userId = UserHolder.getUser().getId();
        Notice notice = getById(id);
        if(notice != null && notice.getUserId().equals(userId)){
            notice.setIsRead(true);
            updateById(notice);
        }
        return Result.ok();
    }

    @Override
    public Result markAllRead() {
        Long userId = UserHolder.getUser().getId();
        lambdaUpdate()
                .eq(Notice::getUserId, userId)
                .set(Notice::getIsRead, true)
                .update();
        return Result.ok();
    }

    @Override
    public Result clearAll() {
        Long userId = UserHolder.getUser().getId();
        remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Notice>()
                .eq("user_id", userId));
        return Result.ok();
    }
}
