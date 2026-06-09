package com.qingjian.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingjian.dto.Result;
import com.qingjian.dto.ScrollResult;
import com.qingjian.dto.UserDTO;
import com.qingjian.entity.Blog;
import com.qingjian.entity.Follow;
import com.qingjian.entity.Notice;
import com.qingjian.entity.Shop;
import com.qingjian.entity.User;
import com.qingjian.entity.UserInfo;
import com.qingjian.mapper.BlogMapper;
import com.qingjian.service.IBlogService;
import com.qingjian.service.IFollowService;
import com.qingjian.service.IShopService;
import com.qingjian.service.IUserService;
import com.qingjian.utils.SystemConstants;
import com.qingjian.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.qingjian.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.qingjian.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Resource
    private IShopService shopService;

    @Resource
    private com.qingjian.service.INoticeService noticeService;

    @Resource
    private com.qingjian.service.IUserInfoService userInfoService;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        if (records.isEmpty()) {
            return Result.ok(records);
        }
        // 批量查询用户和商户信息，避免N+1
        batchQueryBlogUsers(records);
        records.forEach(this::isBlogLiked);
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        // 1.查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        // 2.查询blog有关的用户
        queryBlogUser(blog);
        // 3.查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_KEY + id;//判断用score
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 3.如果未点赞，可以点赞
            // 3.1.数据库点赞数 + 1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 3.2.保存用户到Redis的set集合  zadd key value score
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 4.如果已点赞，取消点赞
            // 4.1.数据库点赞数 -1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            // 4.2.把用户从Redis的set集合移除
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 1.查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key,0,4);
        if(top5 == null || top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        // 2.解析出其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        // 3.根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        List<UserDTO> userDTOS = userService.query().in("id", ids).last("ORDER BY FIELD(id," +idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4.返回
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 2.保存探店笔记
        boolean isSuccess = save(blog);
        if(!isSuccess){
            return Result.fail("新增笔记失败!");
        }
        // 更新笔记数
        userInfoService.lambdaUpdate()
            .setSql("blog_count = IFNULL(blog_count, 0) + 1")
            .eq(UserInfo::getUserId, user.getId())
            .update();
        // 3.分批查询笔记作者的所有粉丝，避免大V粉丝过多导致OOM
        int batchSize = 1000;
        int current = 1;
        while (true) {
            List<Follow> follows = followService.query()
                    .eq("follow_user_id", user.getId())
                    .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, batchSize))
                    .getRecords();
            if (follows.isEmpty()) {
                break;
            }
            // 4.批量推送笔记id给粉丝（使用Pipeline减少RTT）
            stringRedisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (Follow follow : follows) {
                    Long fanId = follow.getUserId();
                    String key = FEED_KEY + fanId;
                    connection.zSetCommands().zAdd(key.getBytes(), System.currentTimeMillis(), blog.getId().toString().getBytes());
                }
                return null;
            });
            // 5.批量发送消息通知（减少数据库交互次数）
            List<Notice> notices = new ArrayList<>();
            for (Follow follow : follows) {
                Notice notice = new Notice();
                notice.setUserId(follow.getUserId());
                notice.setSenderId(user.getId());
                notice.setType(4);
                notice.setContent(user.getNickName() + " 发布了新笔记");
                notice.setRelatedId(blog.getId());
                notice.setIsRead(false);
                notices.add(notice);
            }
            if (!notices.isEmpty()) {
                noticeService.saveBatch(notices);
            }
            current++;
        }
        // 5.返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // 3.非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 4.解析数据：blogId、minTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0; // 2
        int os = 1; // 2
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) { // 5 4 4 2 2
            // 4.1.获取id
            ids.add(Long.valueOf(tuple.getValue()));
            // 4.2.获取分数(时间戳）
            long time = tuple.getScore().longValue();
            if(time == minTime){
                os++;
            }else{
                minTime = time;
                os = 1;
            }
        }

        // 5.根据id查询blog（避免SQL硬拼接，改为内存排序）
        List<Blog> blogs = query().in("id", ids).list();
        // 按ids顺序排序（内存排序替代ORDER BY FIELD）
        Map<Long, Blog> blogMap = blogs.stream().collect(Collectors.toMap(Blog::getId, b -> b));
        List<Blog> sortedBlogs = new ArrayList<>();
        for (Long id : ids) {
            Blog blog = blogMap.get(id);
            if (blog != null) {
                sortedBlogs.add(blog);
            }
        }

        // 6.批量查询用户和点赞状态
        batchQueryBlogUsers(sortedBlogs);
        sortedBlogs.forEach(this::isBlogLiked);

        // 6.封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);

        return Result.ok(r);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        if (user != null) {
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        }

        // 查询商户信息
        Long shopId = blog.getShopId();
        if (shopId != null) {
            Shop shop = shopService.getById(shopId);
            if (shop != null) {
                blog.setShopName(shop.getName());
                blog.setShopArea(shop.getArea());
            }
        }
    }

    /**
     * 批量查询博客的用户和商户信息，避免N+1查询
     */
    private void batchQueryBlogUsers(List<Blog> blogs) {
        // 收集所有用户ID和商户ID
        List<Long> userIds = blogs.stream().map(Blog::getUserId).distinct().collect(Collectors.toList());
        List<Long> shopIds = blogs.stream().map(Blog::getShopId).filter(id -> id != null).distinct().collect(Collectors.toList());

        // 批量查询
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Shop> shopMap = shopIds.isEmpty() ? Collections.emptyMap()
                : shopService.listByIds(shopIds).stream().collect(Collectors.toMap(Shop::getId, s -> s));

        // 组装数据
        for (Blog blog : blogs) {
            User user = userMap.get(blog.getUserId());
            if (user != null) {
                blog.setName(user.getNickName());
                blog.setIcon(user.getIcon());
            }
            Shop shop = shopMap.get(blog.getShopId());
            if (shop != null) {
                blog.setShopName(shop.getName());
                blog.setShopArea(shop.getArea());
            }
        }
    }

    @Override
    public Result deleteBlog(Long id) {
        // 1. 获取当前登录用户
        UserDTO user = UserHolder.getUser();

        // 2. 查询博客信息
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("博客不存在");
        }

        // 3. 校验是否是自己的博客
        if (!blog.getUserId().equals(user.getId())) {
            return Result.fail("无权删除他人博客");
        }

        // 4. 删除博客
        removeById(id);

        // 5. 清理 Redis 中的点赞记录
        stringRedisTemplate.delete(BLOG_LIKED_KEY + id);

        // 6. 清理粉丝Feed流中的该博客（分批处理避免OOM）
        int batchSize = 1000;
        int currentPage = 1;
        while (true) {
            List<Follow> follows = followService.query()
                    .eq("follow_user_id", user.getId())
                    .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(currentPage, batchSize))
                    .getRecords();
            if (follows.isEmpty()) {
                break;
            }
            for (Follow follow : follows) {
                String key = FEED_KEY + follow.getUserId();
                stringRedisTemplate.opsForZSet().remove(key, id.toString());
            }
            currentPage++;
        }

        // 7. 清理相关通知
        noticeService.lambdaUpdate()
                .eq(Notice::getRelatedId, id)
                .eq(Notice::getType, 4)
                .remove();

        // 8. 更新用户笔记数
        userInfoService.lambdaUpdate()
                .setSql("blog_count = IFNULL(blog_count, 0) - 1")
                .eq(UserInfo::getUserId, user.getId())
                .apply("blog_count >= {0}", 1) // 防止减到负数
                .update();

        return Result.ok();
    }
}
