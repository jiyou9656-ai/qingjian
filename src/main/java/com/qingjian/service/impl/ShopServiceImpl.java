package com.qingjian.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingjian.dto.Result;
import com.qingjian.entity.Shop;
import com.qingjian.mapper.ShopMapper;
import com.qingjian.service.IShopService;
import com.qingjian.utils.CacheClient;
import com.qingjian.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qingjian.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {

        // 解决缓存穿透
        Shop shop = cacheClient
                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

//        // 逻辑过期解决缓存击穿
//         Shop shop = cacheClient
//                 .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES );
        if (shop==null){
            return Result.fail("店铺不存在");
        }
                // 7.返回
        return Result.ok(shop);
    }



    private Boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().
                setIfAbsent(key, "1", 10, TimeUnit.SECONDS);

        return BooleanUtil.isTrue(flag);
    }
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
    //可以把前面缓存穿透的代码封装


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 只允许更新部分字段，防止前端传入不该修改的字段
        lambdaUpdate()
                .set(shop.getName() != null, Shop::getName, shop.getName())
                .set(shop.getTypeId() != null, Shop::getTypeId, shop.getTypeId())
                .set(shop.getImages() != null, Shop::getImages, shop.getImages())
                .set(shop.getArea() != null, Shop::getArea, shop.getArea())
                .set(shop.getAddress() != null, Shop::getAddress, shop.getAddress())
                .set(shop.getX() != null, Shop::getX, shop.getX())
                .set(shop.getY() != null, Shop::getY, shop.getY())
                .set(shop.getAvgPrice() != null, Shop::getAvgPrice, shop.getAvgPrice())
                .set(shop.getSold() != null, Shop::getSold, shop.getSold())
                .set(shop.getComments() != null, Shop::getComments, shop.getComments())
                .set(shop.getScore() != null, Shop::getScore, shop.getScore())
                .set(shop.getOpenHours() != null, Shop::getOpenHours, shop.getOpenHours())
                .eq(Shop::getId, id)
                .update();
        // 2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y, String sortBy) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page;
            if (typeId == 0) {
                // typeId为0时查询所有类型
                page = query()
                        .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            } else {
                // 根据sortBy参数排序
                if (StrUtil.isNotBlank(sortBy)) {
                    if ("comments".equals(sortBy)) {
                        page = query()
                                .eq("type_id", typeId)
                                .orderByDesc("comments")
                                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
                    } else if ("score".equals(sortBy)) {
                        page = query()
                                .eq("type_id", typeId)
                                .orderByDesc("score")
                                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
                    } else {
                        // 默认按距离（无坐标时不排序）
                        page = query()
                                .eq("type_id", typeId)
                                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
                    }
                } else {
                    page = query()
                            .eq("type_id", typeId)
                            .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
                }
            }
            // 返回数据
            log.info("返回的type/of/quershopByType的数据: {}", page.getRecords());
            return Result.ok(page.getRecords());
        }

        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo() // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        // 4.解析出id
        if (results == null || results.getContent().isEmpty()) {
            // Redis中没有数据，降级到数据库查询并计算距离
            Page<Shop> page;
            if (typeId == 0) {
                page = query()
                        .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            } else {
                if (StrUtil.isNotBlank(sortBy)) {
                    if ("comments".equals(sortBy)) {
                        page = query()
                                .eq("type_id", typeId)
                                .orderByDesc("comments")
                                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
                    } else if ("score".equals(sortBy)) {
                        page = query()
                                .eq("type_id", typeId)
                                .orderByDesc("score")
                                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
                    } else {
                        page = query()
                                .eq("type_id", typeId)
                                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
                    }
                } else {
                    page = query()
                            .eq("type_id", typeId)
                            .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
                }
            }
            List<Shop> shops = page.getRecords();
            for (Shop shop : shops) {
                double distance = calculateDistance(x, y, shop.getX(), shop.getY());
                shop.setDistance(distance);
            }
            return Result.ok(shops);
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5.根据id查询Shop（避免SQL硬拼接，改为内存排序）
        List<Shop> shops = query().in("id", ids).list();
        Map<Long, Shop> shopMap = shops.stream().collect(Collectors.toMap(Shop::getId, s -> s));
        List<Shop> sortedShops = new ArrayList<>();
        for (Long id : ids) {
            Shop shop = shopMap.get(id);
            if (shop != null) {
                shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
                sortedShops.add(shop);
            }
        }
        // 6.返回
        return Result.ok(sortedShops);
    }
    
    /**
     * Haversine公式计算球面距离（单位：米）
     */
    private double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public Result queryNearbyShops(Integer current, Double x, Double y, String sortBy) {
        // 1. 如果没有坐标，直接查数据库
        if (x == null || y == null) {
            Page<Shop> page = query()
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        // 2. 从数据库查询所有商户并计算距离
        Page<Shop> page = query()
                .page(new Page<>(1, 200)); // 先查一批数据
        List<Shop> shops = page.getRecords();
        for (Shop shop : shops) {
            double distance = calculateDistance(x, y, shop.getX(), shop.getY());
            shop.setDistance(distance);
        }
        // 按距离排序
        shops.sort(Comparator.comparingDouble(Shop::getDistance));

        // 3. 分页
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = Math.min(from + SystemConstants.DEFAULT_PAGE_SIZE, shops.size());
        if (from >= shops.size()) {
            return Result.ok(Collections.emptyList());
        }
        List<Shop> pagedShops = shops.subList(from, end);

        return Result.ok(pagedShops);
    }

    @Override
    public Result recommendShops(Double x, Double y) {
        // AI 智能推荐：综合距离、评分、销量计算推荐分
        // 1. 如果没有坐标，直接返回热门商户
        if (x == null || y == null) {
            List<Shop> shops = query()
                    .orderByDesc("sold", "score")
                    .last("LIMIT 10")
                    .list();
            return Result.ok(shops);
        }

        // 2. 分页查询商户，避免全表加载OOM，内存中只保留TOP 10候选
        int pageSize = 1000;
        int currentPage = 1;
        double maxDistance = 0, maxSold = 0, maxScore = 0;
        // 用优先队列维护TOP 10推荐（按推荐分排序）
        java.util.PriorityQueue<Shop> topShops = new java.util.PriorityQueue<>(
                11, (a, b) -> Double.compare(a.getDistance(), b.getDistance())
        );

        while (true) {
            Page<Shop> page = query().page(new Page<>(currentPage, pageSize));
            List<Shop> shops = page.getRecords();
            if (shops.isEmpty()) {
                break;
            }
            for (Shop shop : shops) {
                double dist = calculateDistance(x, y, shop.getX(), shop.getY());
                maxDistance = Math.max(maxDistance, dist);
                maxSold = Math.max(maxSold, shop.getSold() == null ? 0 : shop.getSold());
                maxScore = Math.max(maxScore, shop.getScore() == null ? 0 : shop.getScore());
                shop.setDistance(dist); // 临时存真实距离
                topShops.offer(shop);
                if (topShops.size() > 10) {
                    topShops.poll(); // 淘汰推荐分最低的
                }
            }
            if (!page.hasNext()) {
                break;
            }
            currentPage++;
        }

        // 3. 从优先队列取出TOP 10，计算最终推荐分并排序
        List<Shop> recommended = new ArrayList<>();
        while (!topShops.isEmpty()) {
            Shop shop = topShops.poll();
            double dist = shop.getDistance();
            double distScore = maxDistance > 0 ? 1 - (dist / maxDistance) : 1;
            double scoreRate = maxScore > 0 ? shop.getScore() / maxScore : 0;
            double soldRate = maxSold > 0 ? (shop.getSold() == null ? 0 : shop.getSold()) / maxSold : 0;
            double recommendScore = distScore * 0.3 + scoreRate * 0.4 + soldRate * 0.3;
            shop.setDistance(dist); // 恢复真实距离
            recommended.add(shop);
        }
        // 按推荐分降序排列（Lambda只能引用final变量，复制一份）
        final double finalMaxDistance = maxDistance;
        final double finalMaxScore = maxScore;
        final double finalMaxSold = maxSold;
        recommended.sort((a, b) -> Double.compare(
                (1 - b.getDistance() / finalMaxDistance) * 0.3 + (b.getScore() / finalMaxScore) * 0.4 + ((b.getSold() == null ? 0 : b.getSold()) / finalMaxSold) * 0.3,
                (1 - a.getDistance() / finalMaxDistance) * 0.3 + (a.getScore() / finalMaxScore) * 0.4 + ((a.getSold() == null ? 0 : a.getSold()) / finalMaxSold) * 0.3
        ));

        return Result.ok(recommended);
    }

    @Override
    public Result queryShopRank(Integer current) {
        // 点评榜单：按评论数 + 评分综合排序
        Page<Shop> page = new Page<>(current, 10);
        page = query()
                .orderByDesc("comments")
                .orderByDesc("score")
                .page(page);

        // 计算排名
        List<Shop> shops = page.getRecords();
        for (int i = 0; i < shops.size(); i++) {
            shops.get(i).setRank((current - 1) * 10 + i + 1);
        }

        return Result.ok(shops);
    }
}
