package com.qingjian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingjian.dto.Result;
import com.qingjian.entity.ChatMessage;
import com.qingjian.entity.User;
import com.qingjian.mapper.ChatMessageMapper;
import com.qingjian.service.IChatMessageService;
import com.qingjian.service.IUserService;
import com.qingjian.service.IFollowService;
import com.qingjian.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 聊天消息服务实现类
 * </p>
 *
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatMessageService {

    @Resource
    private IUserService userService;

    @Resource
    private IFollowService followService;

    @Override
    public Result sendMessage(Long receiverId, String content) {
        Long senderId = UserHolder.getUser().getId();
        
        if (senderId.equals(receiverId)) {
            return Result.fail("不能给自己发送消息");
        }
        
        // 检查是否已关注对方
        long followCount = followService.lambdaQuery()
                .eq(com.qingjian.entity.Follow::getUserId, senderId)
                .eq(com.qingjian.entity.Follow::getFollowUserId, receiverId)
                .count();
        
        boolean isFollowed = followCount > 0;
        
        if (!isFollowed) {
            // 未关注：检查是否已经发过消息
            long msgCount = lambdaQuery()
                    .eq(ChatMessage::getSenderId, senderId)
                    .eq(ChatMessage::getReceiverId, receiverId)
                    .count();
            
            if (msgCount >= 1) {
                return Result.fail("请先关注对方才能继续聊天");
            }
        }
        
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setIsRead(false);
        save(message);
        
        return Result.ok(message);
    }

    @Override
    public Result getChatHistory(Long otherUserId, Integer current) {
        Long userId = UserHolder.getUser().getId();
        
        Page<ChatMessage> page = query()
                .and(w -> w
                        .eq("sender_id", userId).eq("receiver_id", otherUserId)
                        .or()
                        .eq("sender_id", otherUserId).eq("receiver_id", userId))
                .orderByDesc("create_time")
                .page(new Page<>(current, 20));
        
        List<ChatMessage> messages = page.getRecords();
        if (messages.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        
        // 将发送给当前用户的消息标记为已读
        List<Long> unreadIds = messages.stream()
                .filter(m -> m.getReceiverId().equals(userId) && !m.getIsRead())
                .map(ChatMessage::getId)
                .collect(Collectors.toList());
        
        if (!unreadIds.isEmpty()) {
            lambdaUpdate()
                    .in(ChatMessage::getId, unreadIds)
                    .set(ChatMessage::getIsRead, true)
                    .update();
        }
        
        // 填充发送者信息
        List<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .collect(Collectors.toList());
        
        if (!senderIds.isEmpty()) {
            Map<Long, User> userMap = userService.listByIds(senderIds)
                    .stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            messages.forEach(msg -> {
                User user = userMap.get(msg.getSenderId());
                if (user != null) {
                    msg.setSenderIcon(user.getIcon());
                    msg.setSenderNickName(user.getNickName());
                }
            });
        }
        
        // 反转列表，让旧消息在前
        Collections.reverse(messages);
        
        return Result.ok(messages);
    }

    @Override
    public Result getChatList() {
        Long userId = UserHolder.getUser().getId();
        
        // 获取所有与当前用户相关的消息
        List<ChatMessage> allMessages = query()
                .and(w -> w
                        .eq("sender_id", userId)
                        .or()
                        .eq("receiver_id", userId))
                .orderByDesc("create_time")
                .list();
        
        if (allMessages.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        
        // 按对方用户ID分组，取每个用户的最后一条消息
        Map<Long, ChatMessage> latestMessages = new LinkedHashMap<>();
        for (ChatMessage msg : allMessages) {
            Long otherUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            if (!latestMessages.containsKey(otherUserId)) {
                latestMessages.put(otherUserId, msg);
            }
        }
        
        // 收集所有对方用户ID
        List<Long> otherUserIds = new ArrayList<>(latestMessages.keySet());
        Map<Long, User> userMap = userService.listByIds(otherUserIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        
        // 构建聊天列表
        List<Map<String, Object>> chatList = new ArrayList<>();
        for (Map.Entry<Long, ChatMessage> entry : latestMessages.entrySet()) {
            Long otherUserId = entry.getKey();
            ChatMessage lastMsg = entry.getValue();
            User otherUser = userMap.get(otherUserId);
            
            Map<String, Object> chatItem = new HashMap<>();
            chatItem.put("userId", otherUserId);
            chatItem.put("icon", otherUser != null ? otherUser.getIcon() : "");
            chatItem.put("nickName", otherUser != null ? otherUser.getNickName() : "用户");
            chatItem.put("lastMessage", lastMsg.getContent());
            chatItem.put("createTime", lastMsg.getCreateTime());
            
            // 计算未读消息数
            long unreadCount = allMessages.stream()
                    .filter(m -> m.getReceiverId().equals(userId) 
                            && m.getSenderId().equals(otherUserId) 
                            && !m.getIsRead())
                    .count();
            chatItem.put("unreadCount", unreadCount);
            
            chatList.add(chatItem);
        }
        
        return Result.ok(chatList);
    }

    @Override
    public Result markRead(Long otherUserId) {
        Long userId = UserHolder.getUser().getId();
        
        lambdaUpdate()
                .eq(ChatMessage::getSenderId, otherUserId)
                .eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getIsRead, false)
                .set(ChatMessage::getIsRead, true)
                .update();
        
        return Result.ok();
    }

    @Override
    public Result getUnreadCount() {
        Long userId = UserHolder.getUser().getId();
        
        Long count = Long.valueOf(lambdaQuery()
                .eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getIsRead, false)
                .count());
        
        return Result.ok(count);
    }

    @Override
    public Result deleteChat(Long otherUserId) {
        Long userId = UserHolder.getUser().getId();
        
        remove(new QueryWrapper<ChatMessage>()
                .and(w -> w
                        .eq("sender_id", userId).eq("receiver_id", otherUserId)
                        .or()
                        .eq("sender_id", otherUserId).eq("receiver_id", userId)));
        
        return Result.ok();
    }

    @Override
    public Result clearAllChats() {
        Long userId = UserHolder.getUser().getId();
        
        remove(new QueryWrapper<ChatMessage>()
                .eq("sender_id", userId)
                .or()
                .eq("receiver_id", userId));
        
        return Result.ok();
    }
}
