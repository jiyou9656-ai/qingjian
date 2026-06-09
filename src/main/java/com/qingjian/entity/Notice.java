package com.qingjian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_notice")
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接收消息的用户ID
     */
    private Long userId;

    /**
     * 消息类型：1-点赞 2-评论 3-关注
     */
    private Integer type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 关联ID（如博客ID）
     */
    private Long relatedId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 是否已读：0-未读 1-已读
     */
    @TableField("is_read")
    private Boolean isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 发送者头像（非数据库字段）
     */
    @TableField(exist = false)
    private String icon;

    /**
     * 发送者昵称（非数据库字段）
     */
    @TableField(exist = false)
    private String nickname;
}
