import React, { useState } from 'react';
import { View, Text, ScrollView } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';
import ShopCard from '@/components/ShopCard';
import { shops, categories } from '@/data/shops';

const HomePage: React.FC = () => {
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);

  console.log('[Home] Page loaded');

  const filteredShops = selectedCategory
    ? shops.filter((s) => {
        const cat = categories.find((c) => c.id === selectedCategory);
        return cat && s.type.includes(cat.name);
      })
    : shops;

  const seckillShops = shops.filter((s) => s.isSeckill);

  return (
    <ScrollView className={styles.homePage} scrollY>
      <View className={styles.header}>
        <View className={styles.searchBar}>
          <Text className={styles.searchIcon}>🔍</Text>
          <Text className={styles.searchText}>搜索商户、笔记</Text>
        </View>
      </View>

      <View className={styles.categories}>
        {categories.map((cat) => (
          <View
            key={cat.id}
            className={styles.categoryItem}
            onClick={() => setSelectedCategory(cat.id)}
          >
            <Text className={styles.icon}>{cat.icon}</Text>
            <Text className={styles.name}>{cat.name}</Text>
          </View>
        ))}
      </View>

      {seckillShops.length > 0 && (
        <View className={styles.seckillSection}>
          <View className={styles.header}>
            <Text className={styles.title}>⚡ 限时秒杀</Text>
            <Text className={styles.more}>查看全部 ›</Text>
          </View>
          <View className={styles.voucherList}>
            {seckillShops.slice(0, 3).map((shop) => (
              <View key={shop.id} className={styles.voucherItem}>
                <Text className={styles.shopName}>{shop.name}</Text>
                <Text className={styles.price}>¥{Math.floor(shop.avgPrice * 0.5)}</Text>
                <Text className={styles.originalPrice}>¥{shop.avgPrice}</Text>
              </View>
            ))}
          </View>
        </View>
      )}

      <Text className={styles.sectionTitle}>推荐商户</Text>
      <View className={styles.shopList}>
        {filteredShops.map((shop) => (
          <ShopCard key={shop.id} shop={shop} />
        ))}
      </View>
    </ScrollView>
  );
};

export default HomePage;
