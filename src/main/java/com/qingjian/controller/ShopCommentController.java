package com.qingjian.controller;

import com.qingjian.dto.Result;
import com.qingjian.service.IShopCommentService;
import com.qingjian.service.IShopCommentTagService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * <p>
 * 商户评价 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/shop-comment")
@Validated
public class ShopCommentController {

    @Resource
    private IShopCommentService shopCommentService;

    @Resource
    private IShopCommentTagService shopCommentTagService;

    /**
     * 查询商户评价列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryComments(
            @PathVariable("shopId") @NotNull(message = "商户ID不能为空") Long shopId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于等于1") @Max(value = 50, message = "每页大小不能超过50") Integer size) {
        return shopCommentService.queryShopComments(shopId, current, size);
    }

    /**
     * 发布评价
     */
    @PostMapping
    public Result saveComment(@Valid @RequestBody ShopCommentDTO dto) {
        return shopCommentService.saveComment(dto.getShopId(), dto.getScore(), dto.getContent(), dto.getTags());
    }

    /**
     * 点赞评价
     */
    @PostMapping("/like/{id}")
    public Result likeComment(@PathVariable("id") @NotNull(message = "评价ID不能为空") Long commentId) {
        return shopCommentService.likeComment(commentId);
    }

    /**
     * 查询商户评价标签统计
     */
    @GetMapping("/tags/{shopId}")
    public Result queryTags(@PathVariable("shopId") @NotNull(message = "商户ID不能为空") Long shopId) {
        return shopCommentTagService.queryShopCommentTags(shopId);
    }

    /**
     * 接收评价数据的 DTO
     */
    public static class ShopCommentDTO {
        @NotNull(message = "商户ID不能为空")
        private Long shopId;

        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分最小为1")
        @Max(value = 5, message = "评分最大为5")
        private Integer score;

        @NotBlank(message = "评价内容不能为空")
        @Size(max = 512, message = "评价内容不能超过512字")
        private String content;

        private String tags;

        public Long getShopId() { return shopId; }
        public void setShopId(Long shopId) { this.shopId = shopId; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }
}
