package com.qingjian.controller;


import com.qingjian.dto.Result;
import com.qingjian.service.INoticeService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/notice")
public class NoticeController {
    @Resource
    private INoticeService noticeService;

    @GetMapping("/list")
    public Result list(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return noticeService.list(current);
    }

    @GetMapping("/count")
    public Result countUnread() {
        return noticeService.countUnread();
    }

    @PutMapping("/read/{id}")
    public Result markRead(@PathVariable("id") Long id) {
        return noticeService.markRead(id);
    }

    @PutMapping("/read/all")
    public Result markAllRead() {
        return noticeService.markAllRead();
    }

    @DeleteMapping("/clear/all")
    public Result clearAll() {
        return noticeService.clearAll();
    }
}
