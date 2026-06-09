package com.qingjian.util;

import com.qingjian.entity.Shop;
import com.qingjian.service.IShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class DataInitUtil implements CommandLineRunner {

    @Autowired
    private IShopService shopService;

    private Random random = new Random();

    // 各类型商户名称模板
    private String[][] shopNames = {
            // typeId=3 丽人·美发
            {"艺造型", "时尚沙龙", "魅力造型", "美发殿堂", "潮流发艺", "创意美发", "名流造型", "时尚剪吧", "完美造型", "魅力发廊",
             "顶尖造型", "时尚工坊", "创意沙龙", "名流发艺", "潮流剪艺", "完美发廊", "时尚造型", "魅力工坊", "创意剪吧", "顶尖沙龙",
             "美丽人生", "时尚密码", "潮流前线", "魅力无限", "创意空间", "名流会所", "完美形象", "时尚先锋", "魅力之都", "创意天地"},
            // typeId=4 美睫·美甲
            {"指尖艺术", "美甲工坊", "纤纤美甲", "炫彩美甲", "美甲天堂", "美丽指尖", "美甲沙龙", "纤纤玉指", "炫彩指尖", "美甲风尚",
             "指尖诱惑", "美甲潮流", "纤纤美指", "炫彩工坊", "美甲空间", "美丽风尚", "美甲艺术", "纤纤沙龙", "炫彩天堂", "美甲无限",
             "指尖传奇", "美甲先锋", "纤纤创意", "炫彩密码", "美甲天地", "美丽密码", "美甲工坊", "纤纤风尚", "炫彩艺术", "美甲魅力"},
            // typeId=5 按摩·足疗
            {"养生阁", "健康坊", "足浴会所", "按摩世家", "养生堂", "足道馆", "推拿坊", "足疗天地", "按摩乐园", "养生会所",
             "健康驿站", "足浴城", "推拿馆", "足道坊", "按摩宫", "养生馆", "健康会所", "足浴坊", "推拿城", "足道馆",
             "按摩院", "养生中心", "健康馆", "足浴天地", "推拿会所", "足道城", "按摩会所", "养生坊", "健康天地", "足浴中心"},
            // typeId=6 美容SPA
            {"美丽人生", "SPA会所", "美容工坊", "养生SPA", "美颜坊", "SPA天地", "美容中心", "养生会所", "美颜馆", "SPA沙龙",
             "美丽坊", "SPA工坊", "美容会所", "养生馆", "美颜天地", "SPA中心", "美丽中心", "SPA馆", "美容沙龙", "养生天地",
             "美颜会所", "SPA坊", "美丽馆", "SPA中心", "美容工坊", "养生馆", "美颜沙龙", "SPA天地", "美丽会所", "养生中心"},
            // typeId=7 亲子游乐
            {"童趣乐园", "亲子天地", "欢乐童年", "儿童乐园", "亲子乐园", "童趣坊", "欢乐天地", "儿童世界", "亲子坊", "童趣城",
             "欢乐童年", "儿童乐园", "亲子天地", "童趣馆", "欢乐坊", "儿童中心", "亲子城", "童趣乐园", "欢乐馆", "儿童坊",
             "亲子乐园", "童趣天地", "欢乐城", "儿童天地", "亲子馆", "童趣坊", "欢乐乐园", "儿童城", "亲子中心", "童趣世界"},
            // typeId=8 酒吧
            {"夜色酒吧", "星光酒吧", "浪漫酒吧", "音乐酒吧", "潮流酒吧", "夜色会所", "星光之夜", "浪漫之约", "音乐工坊", "潮流派对",
             "夜色倾城", "星光灿烂", "浪漫满屋", "音乐天堂", "潮流前线", "夜色阑珊", "星光大道", "浪漫时光", "音乐空间", "潮流圣地",
             "夜色迷人", "星光闪耀", "浪漫风情", "音乐殿堂", "潮流酒吧", "夜色酒吧", "星光酒吧", "浪漫酒吧", "音乐酒吧", "潮流派对"},
            // typeId=9 轰趴馆
            {"欢乐派对", "聚会天堂", "派对空间", "轰趴乐园", "聚会工坊", "派对城", "欢乐天地", "聚会中心", "派对馆", "轰趴天地",
             "欢乐派对", "聚会坊", "派对乐园", "轰趴馆", "聚会天地", "派对空间", "欢乐中心", "聚会馆", "派对工坊", "轰趴城",
             "欢乐聚会", "派对天地", "聚会乐园", "轰趴空间", "聚会派对", "派对馆", "欢乐馆", "聚会城", "派对中心", "轰趴乐园"},
            // typeId=10 健身运动
            {"动感健身", "活力健身房", "运动天地", "健身工坊", "活力中心", "运动馆", "动感空间", "健身会所", "活力天地", "运动工坊",
             "动感地带", "健身中心", "活力馆", "运动城", "健身馆", "活力空间", "运动中心", "健身天地", "活力工坊", "运动会所",
             "动感健身", "健身乐园", "活力天地", "运动馆", "健身工坊", "活力城", "运动中心", "健身会所", "活力空间", "运动天地"}
    };

    // 商圈列表
    private String[] areas = {"拱宸桥", "运河上街", "大关", "北部新城", "水晶城", "远洋乐堤港", "D32天阳购物中心", "上塘", "和睦", "米市巷"};

    // 地址模板（按类型）
    private String[][] addresses = {
            // typeId=3 丽人·美发
            {"拱宸桥街道金华路32号远洋乐堤港1层101", "大关街道上塘路158号水晶城购物中心F2", "拱宸桥街道台州路66号远洋乐堤港B1层", 
             "和睦街道丽水路45号", "拱宸桥街道湖州街288号", "上塘街道运河上街购物中心3层"},
            // typeId=4 美睫·美甲
            {"拱宸桥街道金华路18号", "大关街道水晶城购物中心F3层305", "上塘街道台州路128号", 
             "运河上街丽水路99号远洋乐堤港2层", "拱宸桥街道湖州街368号", "北部新城万达广场B1层"},
            // typeId=5 按摩·足疗
            {"拱宸桥街道金华路56号", "大关街道上塘路228号", "水晶城街道台州路188号远洋乐堤港F3", 
             "和睦街道丽水路78号", "拱宸桥街道湖州街168号", "上塘街道运河上街购物中心B2层"},
            // typeId=6 美容SPA
            {"拱宸桥街道金华路88号", "大关街道水晶城购物中心F4层", "上塘街道台州路258号", 
             "运河上街丽水路166号远洋乐堤港3层", "拱宸桥街道湖州街208号", "北部新城万达广场F2层"},
            // typeId=7 亲子游乐
            {"拱宸桥街道金华路128号水晶城F4层", "大关街道上塘路188号", "上塘街道台州路318号", 
             "和睦街道丽水路128号远洋乐堤港4层", "拱宸桥街道湖州街268号", "北部新城万达广场3层"},
            // typeId=8 酒吧
            {"拱宸桥街道金华路188号D32天阳购物中心B1层", "大关街道上塘路88号", "上塘街道台州路388号", 
             "运河上街丽水路198号远洋乐堤港B1层", "拱宸桥街道湖州街328号", "北部新城万达广场B1层"},
            // typeId=9 轰趴馆
            {"拱宸桥街道金华路218号", "大关街道上塘路268号D32天阳购物中心5层", "上塘街道台州路428号", 
             "和睦街道丽水路258号远洋乐堤港5层", "拱宸桥街道湖州街388号", "北部新城万达广场4层"},
            // typeId=10 健身运动
            {"拱宸桥街道金华路258号水晶城F5层", "大关街道上塘路318号", "上塘街道台州路468号", 
             "运河上街丽水路288号远洋乐堤港6层", "拱宸桥街道湖州街428号", "北部新城万达广场F5层"}
    };

    // 本地图片基础目录
    private static final String IMAGE_BASE_DIR = "D:\\JavaProject\\redis-qingjian\\qingjian-front\\imgs\\shops";
    
    // 商家类型对应的目录名
    private String[] typeDirs = {"hair", "nail", "massage", "spa", "kids", "bar", "party", "gym"};
    
    // 生成本地图片路径（使用UUID方式）
    private String generateLocalImagePath(String typeDir) {
        String shopDir = IMAGE_BASE_DIR + File.separator + typeDir;
        File dir = new File(shopDir);
        
        if (!dir.exists()) {
            // 如果目录不存在，返回在线图片作为备用
            return "https://picsum.photos/seed/" + typeDir + random.nextInt(10) + "/400/300";
        }
        
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jpg"));
        if (files == null || files.length == 0) {
            return "https://picsum.photos/seed/" + typeDir + random.nextInt(10) + "/400/300";
        }
        
        // 随机选择一个图片
        File selectedFile = files[random.nextInt(files.length)];
        
        // 使用作者的方式：UUID生成唯一路径
        String uuid = UUID.randomUUID().toString();
        int hash = uuid.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        
        // 复制并重命名到blogs风格的目录结构
        String newFileName = uuid + ".jpg";
        String targetDir = IMAGE_BASE_DIR + File.separator + "generated" + File.separator + d1 + File.separator + d2;
        new File(targetDir).mkdirs();
        
        try {
            java.nio.file.Files.copy(selectedFile.toPath(), new File(targetDir, newFileName).toPath(), 
                                   java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/imgs/shops/generated/" + d1 + "/" + d2 + "/" + newFileName;
        } catch (Exception e) {
            // 如果复制失败，直接返回原文件路径
            return "/imgs/shops/" + typeDir + "/" + selectedFile.getName();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        // 为每个类型插入30条数据
        int[] typeIds = {3, 4, 5, 6, 7, 8, 9, 10};
        
        for (int i = 0; i < typeIds.length; i++) {
            int typeId = typeIds[i];
            String[] names = shopNames[i];
            String typeDir = typeDirs[i];
            
            List<Shop> shops = new ArrayList<>();
            for (int j = 0; j < names.length; j++) {
                Shop shop = new Shop();
                shop.setName(names[j]);
                shop.setTypeId((long) typeId);
                // 使用本地图片路径（带UUID方式）
                String img1 = generateLocalImagePath(typeDir);
                String img2 = generateLocalImagePath(typeDir);
                String img3 = generateLocalImagePath(typeDir);
                shop.setImages(img1 + "," + img2 + "," + img3);
                shop.setArea(areas[random.nextInt(areas.length)]);
                
                // 根据typeId获取地址模板
                int addrIndex = typeId - 3; // typeId=3对应index=0
                String[] addrTemplates = addresses[addrIndex];
                shop.setAddress(addrTemplates[random.nextInt(addrTemplates.length)]);
                
                // 杭州经纬度附近随机偏移
                shop.setX(120.14 + (random.nextDouble() - 0.5) * 0.05);
                shop.setY(30.32 + (random.nextDouble() - 0.5) * 0.05);
                
                shop.setAvgPrice((long) (random.nextInt(200) + 30));
                shop.setSold(random.nextInt(10000) + 100);
                shop.setComments(random.nextInt(5000) + 50);
                shop.setScore(random.nextInt(20) + 30); // 30-50分
                shop.setOpenHours("10:00-22:00");
                shop.setCreateTime(LocalDateTime.now());
                shop.setUpdateTime(LocalDateTime.now());
                
                shops.add(shop);
            }
            
            shopService.saveBatch(shops);
            System.out.println("已为类型 " + typeId + " 插入 " + shops.size() + " 条数据");
        }
        
        System.out.println("数据初始化完成！");
    }
}
