package com.qingjian.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingjian.dto.Result;
import com.qingjian.entity.Shop;
import com.qingjian.service.IShopService;
import com.qingjian.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 */
@Slf4j
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryById(id);
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 写入数据库
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 写入数据库
        return shopService.update(shop);
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam(value = "typeId", required = false) Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y,
            @RequestParam(value = "sortBy", required = false) String sortBy
    ) {
       return shopService.queryShopByType(typeId == null ? 0 : typeId, current, x, y, sortBy);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        log.info("shop/of/name,name={}", page.getRecords());
        // 返回数据
        return Result.ok(page.getRecords());
    }

    /**
     * 查询附近商户（不限类型）
     * @param current 页码
     * @param x 经度
     * @param y 纬度
     * @param sortBy 排序方式
     * @return 商铺列表
     */
    @GetMapping("/of/location")
    public Result queryNearbyShops(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y,
            @RequestParam(value = "sortBy", required = false) String sortBy
    ) {
        return shopService.queryNearbyShops(current, x, y, sortBy);
    }

    /**
     * AI智能推荐商户
     * @param x 经度
     * @param y 纬度
     * @return 推荐商户列表
     */
    @GetMapping("/recommend")
    public Result recommendShops(
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {
        return shopService.recommendShops(x, y);
    }

    /**
     * 点评榜单：按评论数和评分排序
     * @param current 页码
     * @return 榜单商户列表
     */
    @GetMapping("/rank")
    public Result queryShopRank(
            @RequestParam(defaultValue = "1") Integer current
    ) {
        return shopService.queryShopRank(current);
    }
}
