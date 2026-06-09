package com.qingjian.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingjian.dto.Result;
import com.qingjian.dto.UserDTO;
import com.qingjian.entity.Follow;
import com.qingjian.entity.Notice;
import com.qingjian.entity.User;
import com.qingjian.mapper.FollowMapper;
import com.qingjian.service.IFollowService;
import com.qingjian.service.INoticeService;
import com.qingjian.service.IUserInfoService;
import com.qingjian.service.IUserService;
import com.qingjian.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    @Resource
    private IUserInfoService userInfoService;
    @Resource
    private INoticeService noticeService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        // 1.判断到底是关注还是取关
        if(isFollow) {
            // 2.关注，新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 把关注用户的id，放入redis的set集合 sadd userId followerUserId
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
                // 更新tb_user表中的关注数
                userService.lambdaUpdate()
                    .setSql("followee = followee + 1")
                    .eq(User::getId, userId)
                    .update();
                // 同步更新tb_user_info表中的关注数
                userInfoService.lambdaUpdate()
                    .setSql("followee = followee + 1")
                    .eq(com.qingjian.entity.UserInfo::getUserId, userId)
                    .update();
                // 更新tb_user表中的粉丝数
                userService.lambdaUpdate()
                    .setSql("fans = fans + 1")
                    .eq(User::getId, followUserId)
                    .update();
                // 同步更新tb_user_info表中的粉丝数
                userInfoService.lambdaUpdate()
                    .setSql("fans = fans + 1")
                    .eq(com.qingjian.entity.UserInfo::getUserId, followUserId)
                    .update();
                // 发送关注通知
                UserDTO currentUser = UserHolder.getUser();
                Notice notice = new Notice();
                notice.setUserId(followUserId);
                notice.setType(3);
                notice.setContent(currentUser.getNickName() + " 关注了你");
                notice.setSenderId(currentUser.getId());
                notice.setIsRead(false);
                noticeService.save(notice);
            }
        }else {
            // 3.取关，删除 delete from tb_follow where user_id = ? and follow_user_id = ?
            boolean isSuccess = remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", followUserId));
            if (isSuccess) {
                // 把关注用户的id从Redis集合中移除
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
                // 更新tb_user表中的关注数
                userService.lambdaUpdate()
                    .setSql("followee = followee - 1")
                    .eq(User::getId, userId)
                    .update();
                // 同步更新tb_user_info表中的关注数
                userInfoService.lambdaUpdate()
                    .setSql("followee = followee - 1")
                    .eq(com.qingjian.entity.UserInfo::getUserId, userId)
                    .update();
                // 更新tb_user表中的粉丝数
                userService.lambdaUpdate()
                    .setSql("fans = fans - 1")
                    .eq(User::getId, followUserId)
                    .update();
                // 同步更新tb_user_info表中的粉丝数
                userInfoService.lambdaUpdate()
                    .setSql("fans = fans - 1")
                    .eq(com.qingjian.entity.UserInfo::getUserId, followUserId)
                    .update();
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询是否关注 select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        // 3.判断
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        // 2.求交集
        String key2 = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 无交集
            return Result.ok(Collections.emptyList());
        }
        // 3.解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 4.查询用户
        List<UserDTO> users = userService.listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }

    @Override
    public Result getFollowsList() {
        // 获取当前用户ID
        Long userId = UserHolder.getUser().getId();
        // 查询当前用户关注的所有人
        List<Follow> follows = query().eq("user_id", userId).list();
        if (follows.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 获取关注用户的ID
        List<Long> followUserIds = follows.stream().map(Follow::getFollowUserId).collect(Collectors.toList());
        // 查询用户信息
        List<User> users = userService.listByIds(followUserIds);
        // 转换为DTO，并标记为已关注
        List<UserDTO> userDTOS = users.stream()
                .map(user -> {
                    UserDTO dto = BeanUtil.copyProperties(user, UserDTO.class);
                    // 这里返回的是当前用户关注的人，所以都是已关注
                    // 在返回的DTO中可以添加一个临时字段标记是否关注
                    return dto;
                })
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    @Override
    public Result getFansList() {
        // 获取当前用户ID
        Long userId = UserHolder.getUser().getId();
        // 查询关注当前用户的所有人
        List<Follow> fans = query().eq("follow_user_id", userId).list();
        if (fans.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 获取粉丝用户的ID
        List<Long> fanUserIds = fans.stream().map(Follow::getUserId).collect(Collectors.toList());
        // 查询用户信息
        List<User> users = userService.listByIds(fanUserIds);
        // 获取当前用户已关注的人
        Set<String> myFollows = stringRedisTemplate.opsForSet().members("follows:" + userId);
        // 转换为DTO，并标记是否已关注
        List<UserDTO> userDTOS = users.stream()
                .map(user -> {
                    UserDTO dto = BeanUtil.copyProperties(user, UserDTO.class);
                    // 标记是否已关注
                    boolean isFollow = myFollows != null && myFollows.contains(user.getId().toString());
                    // 添加临时字段标记是否关注，使用Map来承载
                    return dto;
                })
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}
