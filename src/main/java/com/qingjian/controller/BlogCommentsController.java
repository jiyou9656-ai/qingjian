package com.qingjian.controller;

import com.qingjian.dto.Result;
import com.qingjian.entity.BlogComments;
import com.qingjian.service.IBlogCommentsService;
import org.springframework.web.bind.annotation.*;

/**
 * 博客评论前端控制器
 */
@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {

    private final IBlogCommentsService blogCommentsService;

    public BlogCommentsController(IBlogCommentsService blogCommentsService) {
        this.blogCommentsService = blogCommentsService;
    }

    /**
     * 查询博客评论列表（附带评论用户信息）
     */
    @GetMapping("/{blogId}")
    public Result queryBlogComments(@PathVariable Long blogId) {
        return blogCommentsService.queryBlogComments(blogId);
    }

    /**
     * 发表评论
     */
    @PostMapping
    public Result saveBlogComment(@RequestBody BlogComments comment) {
        return blogCommentsService.saveBlogComment(comment);
    }

    /**
     * 点赞评论
     */
    @PutMapping("/like/{id}")
    public Result likeBlogComment(@PathVariable Long id) {
        return blogCommentsService.likeBlogComment(id);
    }
}