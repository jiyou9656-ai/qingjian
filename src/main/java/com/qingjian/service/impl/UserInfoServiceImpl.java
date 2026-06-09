package com.qingjian.service.impl;

import com.qingjian.entity.User;
import com.qingjian.entity.UserInfo;
import com.qingjian.mapper.UserInfoMapper;
import com.qingjian.service.IUserInfoService;
import com.qingjian.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.qingjian.utils.RedisConstants.LOGIN_USER_KEY;
import static com.qingjian.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void updateUserInfo(Long userId, UserInfo userInfo, String nickName) {
        // 更新昵称（如果提供了）
        if (nickName != null && !nickName.isEmpty()) {
            User user = userService.getById(userId);
            if (user != null) {
                user.setNickName(nickName);
                userService.updateById(user);
                
                // 更新 Redis 缓存中的昵称
                updateUserCache(userId, nickName, user.getIcon());
            }
        }
        
        // 更新用户详情
        UserInfo existInfo = this.getById(userId);
        if (existInfo == null) {
            // 不存在则新增
            userInfo.setUserId(userId);
            this.save(userInfo);
        } else {
            // 存在则更新
            userInfo.setUserId(userId);
            this.updateById(userInfo);
        }
    }

    private void updateUserCache(Long userId, String nickName, String icon) {
        // 查找所有登录用户的 Redis key
        var keys = stringRedisTemplate.keys(LOGIN_USER_KEY + "*");
        if (keys != null) {
            for (String key : keys) {
                // 获取当前 key 对应的用户 ID
                String cachedUserId = stringRedisTemplate.opsForHash().get(key, "id").toString();
                if (userId.toString().equals(cachedUserId)) {
                    // 更新缓存中的昵称和头像
                    Map<String, Object> updateMap = new HashMap<>();
                    if (nickName != null) {
                        updateMap.put("nickName", nickName);
                    }
                    if (icon != null) {
                        updateMap.put("icon", icon);
                    }
                    stringRedisTemplate.opsForHash().putAll(key, updateMap);
                    // 刷新有效期
                    stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
                    break;
                }
            }
        }
    }
}
