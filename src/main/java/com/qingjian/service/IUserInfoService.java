package com.qingjian.service;

import com.qingjian.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IUserInfoService extends IService<UserInfo> {

    /**
     * 更新用户信息（包括昵称）
     * @param userId 用户ID
     * @param userInfo 用户详情信息
     * @param nickName 昵称（可选）
     */
    void updateUserInfo(Long userId, UserInfo userInfo, String nickName);
}
