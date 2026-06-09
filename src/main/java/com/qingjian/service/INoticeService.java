package com.qingjian.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qingjian.dto.Result;
import com.qingjian.entity.Notice;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface INoticeService extends IService<Notice> {

    Result list(Integer current);

    Result countUnread();

    Result markRead(Long id);

    Result markAllRead();

    Result clearAll();

}
