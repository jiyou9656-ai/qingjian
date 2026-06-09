package com.qingjian.controller;

import com.qingjian.dto.Result;
import com.qingjian.entity.Shop;
import com.qingjian.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 商户图片更新工具
 */
@Slf4j
@RestController
@RequestMapping("/api/shop-images")
public class ShopImageController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private IShopService shopService;

    // 商户类型配置 (不包含美食和KTV)
    private static final String[][] SHOP_TYPES = {
        {"美发", "hair", "3"},
        {"美甲", "nail", "4"},
        {"足浴按摩", "massage", "5"},
        {"SPA美容", "spa", "6"},
        {"儿童亲子", "kids", "7"},
        {"酒吧", "bar", "8"},
        {"轰趴聚会", "party", "9"},
        {"健身", "gym", "10"}
    };

    private static final String IMAGE_BASE_DIR = "D:\\JavaProject\\redis-qingjian\\qingjian-front\\imgs";

    /**
     * 【简单方案】直接更新为在线图片URL，立即可用
     * 只更新typeId 3-10（除美食和KTV外的）
     */
    @GetMapping("/quick-fix")
    public Result quickFix() {
        int totalUpdated = 0;

        for (String[] type : SHOP_TYPES) {
            String typeName = type[0];
            String seed = type[1];
            int typeId = Integer.parseInt(type[2]);

            List<Shop> shops = shopService.lambdaQuery()
                    .eq(Shop::getTypeId, typeId)
                    .orderByAsc(Shop::getId)
                    .list();

            log.info("处理 {} (typeId={})，共 {} 个商户", typeName, typeId, shops.size());

            for (int i = 0; i < shops.size(); i++) {
                Shop shop = shops.get(i);
                // 使用多个在线图片源，逗号分隔
                String imageUrls = String.format(
                    "https://picsum.photos/seed/%s%d/400/300,https://picsum.photos/seed/%sa%d/400/300,https://picsum.photos/seed/%sb%d/400/300",
                    seed, i + 1, seed, i + 1, seed, i + 1
                );

                try {
                    jdbcTemplate.update("UPDATE tb_shop SET images = ? WHERE id = ?", imageUrls, shop.getId());
                    totalUpdated++;
                } catch (Exception e) {
                    log.error("更新失败: shopId={}", shop.getId(), e);
                }
            }
        }

        return Result.ok("快速修复完成! 已更新 " + totalUpdated + " 条记录，请刷新浏览器!");
    }

    /**
     * 【完整方案】先复制本地图片，再更新为本地路径
     */
    @GetMapping("/full-fix")
    public Result fullFix() {
        // 1. 先收集现有图片
        List<String> existingImages = new ArrayList<>();
        Path blogsPath = Paths.get(IMAGE_BASE_DIR, "blogs");
        Path typesPath = Paths.get(IMAGE_BASE_DIR, "types");

        try {
            if (Files.exists(blogsPath)) {
                try (Stream<Path> stream = Files.walk(blogsPath)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> p.toString().toLowerCase().endsWith(".jpg") 
                                    || p.toString().toLowerCase().endsWith(".jpeg")
                                    || p.toString().toLowerCase().endsWith(".png"))
                            .forEach(p -> existingImages.add(p.toString()));
                }
            }
            
            if (Files.exists(typesPath)) {
                File[] typeFiles = typesPath.toFile().listFiles();
                if (typeFiles != null) {
                    for (File f : typeFiles) {
                        if (f.getName().toLowerCase().endsWith(".jpg") 
                                || f.getName().toLowerCase().endsWith(".jpeg")
                                || f.getName().toLowerCase().endsWith(".png")) {
                            existingImages.add(f.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("收集图片失败", e);
        }

        log.info("找到 {} 张现有图片", existingImages.size());

        if (existingImages.isEmpty()) {
            return quickFix(); // 没有本地图片，用在线方案
        }

        // 2. 复制图片到shops目录
        int totalCopied = 0;
        Path shopsBasePath = Paths.get(IMAGE_BASE_DIR, "shops");

        try {
            for (String[] type : SHOP_TYPES) {
                String typeDir = type[1];
                Path destDir = shopsBasePath.resolve(typeDir);
                Files.createDirectories(destDir);

                for (int i = 0; i < 15; i++) {
                    Path srcPath = Paths.get(existingImages.get(i % existingImages.size()));
                    Path destPath = destDir.resolve(typeDir + "_" + (i + 1) + ".jpg");
                    Files.copy(srcPath, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    totalCopied++;
                }
            }
        } catch (IOException e) {
            log.error("复制图片失败", e);
            return quickFix(); // 复制失败，用在线方案
        }

        log.info("复制了 {} 张图片", totalCopied);

        // 3. 更新数据库
        int totalUpdated = 0;
        for (String[] type : SHOP_TYPES) {
            String typeName = type[0];
            String typeDir = type[1];
            int typeId = Integer.parseInt(type[2]);

            List<Shop> shops = shopService.lambdaQuery()
                    .eq(Shop::getTypeId, typeId)
                    .orderByAsc(Shop::getId)
                    .list();

            for (int i = 0; i < shops.size(); i++) {
                Shop shop = shops.get(i);
                int imageIdx = (i % 15) + 1;
                String localPath = "/imgs/shops/" + typeDir + "/" + typeDir + "_" + imageIdx + ".jpg";

                try {
                    jdbcTemplate.update("UPDATE tb_shop SET images = ? WHERE id = ?", localPath, shop.getId());
                    totalUpdated++;
                } catch (Exception e) {
                    log.error("更新失败: shopId={}", shop.getId(), e);
                }
            }
        }

        return Result.ok("完整修复完成! 复制 " + totalCopied + " 张，更新 " + totalUpdated + " 条，请刷新浏览器!");
    }

    /**
     * 更新所有类型为在线图片（包括美食和KTV）
     */
    @GetMapping("/update-urls")
    public Result updateImageUrls() {
        int totalUpdated = 0;

        String[][] allTypes = {
            {"美食", "food", "1"},
            {"KTV", "ktv", "2"},
            {"美发", "hair", "3"},
            {"美甲", "nail", "4"},
            {"足浴按摩", "massage", "5"},
            {"SPA美容", "spa", "6"},
            {"儿童亲子", "kids", "7"},
            {"酒吧", "bar", "8"},
            {"轰趴聚会", "party", "9"},
            {"健身", "gym", "10"}
        };

        for (String[] type : allTypes) {
            String typeName = type[0];
            String seed = type[1];
            int typeId = Integer.parseInt(type[2]);

            List<Shop> shops = shopService.lambdaQuery()
                    .eq(Shop::getTypeId, typeId)
                    .orderByAsc(Shop::getId)
                    .list();

            log.info("处理 {} (typeId={})，共 {} 个商户", typeName, typeId, shops.size());

            for (int i = 0; i < shops.size(); i++) {
                Shop shop = shops.get(i);
                String imageUrls = String.format(
                    "https://picsum.photos/seed/%s%d/400/300,https://picsum.photos/seed/%sa%d/400/300,https://picsum.photos/seed/%sb%d/400/300",
                    seed, i + 1, seed, i + 1, seed, i + 1
                );

                try {
                    jdbcTemplate.update("UPDATE tb_shop SET images = ? WHERE id = ?", imageUrls, shop.getId());
                    totalUpdated++;
                } catch (Exception e) {
                    log.error("更新失败: shopId={}", shop.getId(), e);
                }
            }
        }

        return Result.ok("图片URL更新完成! 共更新 " + totalUpdated + " 条记录");
    }
}
