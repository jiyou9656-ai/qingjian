package com.qingjian.controller;

import com.qingjian.dto.Result;
import com.qingjian.service.IChatMessageService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 聊天消息前端控制器
 * </p>
 *
 */
@RestController
@RequestMapping("/chat")
public class ChatMessageController {

    @Resource
    private IChatMessageService chatMessageService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result sendMessage(@RequestParam("receiverId") Long receiverId,
                              @RequestParam("content") String content) {
        return chatMessageService.sendMessage(receiverId, content);
    }

    /**
     * 获取聊天记录
     */
    @GetMapping("/history")
    public Result getChatHistory(@RequestParam("otherUserId") Long otherUserId,
                                  @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return chatMessageService.getChatHistory(otherUserId, current);
    }

    /**
     * 获取聊天列表
     */
    @GetMapping("/list")
    public Result getChatList() {
        return chatMessageService.getChatList();
    }

    /**
     * 标记已读
     */
    @PutMapping("/read/{otherUserId}")
    public Result markRead(@PathVariable("otherUserId") Long otherUserId) {
        return chatMessageService.markRead(otherUserId);
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread/count")
    public Result getUnreadCount() {
        return chatMessageService.getUnreadCount();
    }

    /**
     * 删除与某用户的聊天记录
     */
    @DeleteMapping("/{otherUserId}")
    public Result deleteChat(@PathVariable("otherUserId") Long otherUserId) {
        return chatMessageService.deleteChat(otherUserId);
    }

    /**
     * 一键清除所有聊天记录
     */
    @DeleteMapping("/clear/all")
    public Result clearAllChats() {
        return chatMessageService.clearAllChats();
    }
}
