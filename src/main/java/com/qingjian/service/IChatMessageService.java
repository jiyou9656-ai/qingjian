package com.qingjian.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qingjian.dto.Result;
import com.qingjian.entity.ChatMessage;

/**
 * <p>
 * 聊天消息服务类
 * </p>
 *
 */
public interface IChatMessageService extends IService<ChatMessage> {

    /**
     * 发送消息
     */
    Result sendMessage(Long receiverId, String content);

    /**
     * 获取与某用户的聊天记录
     */
    Result getChatHistory(Long otherUserId, Integer current);

    /**
     * 获取聊天列表（最近联系人）
     */
    Result getChatList();

    /**
     * 标记消息已读
     */
    Result markRead(Long otherUserId);

    /**
     * 获取未读消息数
     */
    Result getUnreadCount();

    /**
     * 删除与某用户的所有聊天记录
     */
    Result deleteChat(Long otherUserId);

    /**
     * 一键清除所有聊天记录
     */
    Result clearAllChats();
}
