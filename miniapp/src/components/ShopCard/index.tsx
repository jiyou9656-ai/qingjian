import React from 'react';
import { View, Text, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';
import { Shop } from '@/types';
import { formatNumber } from '@/utils';

interface ShopCardProps {
  shop: Shop;
}

const ShopCard: React.FC<ShopCardProps> = ({ shop }) => {
  const handleNavigate = () => {
    console.log('[ShopCard] Navigate to shop detail:', shop.id);
    Taro.navigateTo({
      url: `/pages/shop-detail/index?id=${shop.id}`,
    });
  };

  return (
    <View className={styles.shopCard} onClick={handleNavigate}>
      <View className={styles.imageWrapper}>
        <Image
          className={styles.image}
          src={shop.images[0]}
          mode='aspectFill'
        />
        {shop.isSeckill && (
          <View className={styles.seckillTag}>秒杀</View>
        )}
      </View>
      <View className={styles.content}>
        <Text className={styles.name}>{shop.name}</Text>
        <View className={styles.info}>
          <Text className={styles.type}>{shop.type}</Text>
          <Text className={styles.distance}>{shop.distance}</Text>
        </View>
        <View className={styles.bottom}>
          <View className={styles.score}>
            <Text className={styles.star}>★</Text>
            <Text className={styles.scoreText}>{shop.score}</Text>
            <Text className={styles.comments}>({formatNumber(shop.comments)})</Text>
          </View>
          <Text className={styles.price}>
            ¥{shop.avgPrice}<Text className={styles.unit}>/人</Text>
          </Text>
        </View>
      </View>
    </View>
  );
};

export default ShopCard;
