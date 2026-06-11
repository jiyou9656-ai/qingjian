import React, { useState } from 'react';
import { View, Text, ScrollView, Image, Button } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import styles from './index.module.scss';
import { shops } from '@/data/shops';
import { showToast } from '@/utils';

const mockComments = [
  {
    id: 1,
    userName: '美食达人',
    avatar: 'https://picsum.photos/id/64/200/200',
    score: 5,
    content: '味道很好，服务态度也很棒，推荐！',
    createTime: '2天前',
  },
  {
    id: 2,
    userName: '咖啡控',
    avatar: 'https://picsum.photos/id/91/200/200',
    score: 4,
    content: '环境不错，就是排队时间有点长。',
    createTime: '5天前',
  },
  {
    id: 3,
    userName: '探店小分队',
    avatar: 'https://picsum.photos/id/177/200/200',
    score: 5,
    content: '性价比很高，下次还会再来。',
    createTime: '1周前',
  },
];

const ShopDetailPage: React.FC = () => {
  const router = useRouter();
  const shopId = Number(router.params.id);
  const shop = shops.find((s) => s.id === shopId) || shops[0];

  console.log('[ShopDetail] Shop id:', shopId);

  const handleSeckill = () => {
    console.log('[ShopDetail] Seckill voucher');
    showToast('秒杀成功', 'success');
  };

  return (
    <ScrollView className={styles.shopDetailPage} scrollY>
      <Image className={styles.banner} src={shop.images[0]} mode='aspectFill' />

      <View className={styles.info}>
        <Text className={styles.name}>{shop.name}</Text>
        <View className={styles.tags}>
          <Text className={styles.tag}>{shop.type}</Text>
          <Text className={styles.tag}>{shop.distance}</Text>
        </View>
        <Text className={styles.address}>{shop.address}</Text>
        <View className={styles.score}>
          <Text className={styles.star}>★</Text>
          <Text className={styles.scoreText}>{shop.score}</Text>
          <Text className={styles.comments}>({shop.comments}条评价)</Text>
        </View>
      </View>

      {shop.isSeckill && (
        <View className={styles.section}>
          <Text className={styles.title}>⚡ 限时秒杀</Text>
          <View className={styles.voucherItem}>
            <View className={styles.left}>
              <Text className={styles.voucherTitle}>{shop.name} 5折券</Text>
              <Text className={styles.price}>
                ¥{Math.floor(shop.avgPrice * 0.5)}
                <Text className={styles.original}>¥{shop.avgPrice}</Text>
              </Text>
            </View>
            <Button className={styles.grabBtn} onClick={handleSeckill}>
              立即抢购
            </Button>
          </View>
        </View>
      )}

      <View className={styles.comments}>
        <Text className={styles.title}>网友评价 ({mockComments.length})</Text>
        {mockComments.map((comment) => (
          <View key={comment.id} className={styles.commentItem}>
            <View className={styles.header}>
              <Image className={styles.avatar} src={comment.avatar} mode='aspectFill' />
              <Text className={styles.userName}>{comment.userName}</Text>
              <Text className={styles.score}>{'★'.repeat(comment.score)}</Text>
            </View>
            <Text className={styles.content}>{comment.content}</Text>
            <Text className={styles.time}>{comment.createTime}</Text>
          </View>
        ))}
      </View>
    </ScrollView>
  );
};

export default ShopDetailPage;
