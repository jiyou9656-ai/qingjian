import React from 'react';
import { View, Text, ScrollView, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';

const menuGroups = [
  {
    title: '订单相关',
    items: [
      { icon: '🎫', label: '我的优惠券', url: '' },
      { icon: '📦', label: '秒杀订单', url: '' },
    ],
  },
  {
    title: '内容相关',
    items: [
      { icon: '📝', label: '我的笔记', url: '' },
      { icon: '❤️', label: '我的收藏', url: '' },
      { icon: '👥', label: '我的关注', url: '' },
      { icon: '👀', label: '浏览记录', url: '' },
    ],
  },
  {
    title: '设置',
    items: [
      { icon: '⚙️', label: '设置', url: '' },
      { icon: '❓', label: '帮助与反馈', url: '' },
    ],
  },
];

const MinePage: React.FC = () => {
  console.log('[Mine] Page loaded');

  return (
    <ScrollView className={styles.minePage} scrollY>
      <View className={styles.header}>
        <Image
          className={styles.avatar}
          src='https://picsum.photos/id/64/200/200'
          mode='aspectFill'
        />
        <View className={styles.userInfo}>
          <Text className={styles.nickName}>青荐用户</Text>
          <Text className={styles.phone}>点击登录</Text>
        </View>
      </View>

      <View className={styles.stats}>
        <View className={styles.statItem}>
          <Text className={styles.value}>12</Text>
          <Text className={styles.label}>关注</Text>
        </View>
        <View className={styles.statItem}>
          <Text className={styles.value}>89</Text>
          <Text className={styles.label}>粉丝</Text>
        </View>
        <View className={styles.statItem}>
          <Text className={styles.value}>234</Text>
          <Text className={styles.label}>获赞</Text>
        </View>
      </View>

      <View className={styles.menuList}>
        {menuGroups.map((group, index) => (
          <View key={index} className={styles.menuGroup}>
            {group.items.map((item, itemIndex) => (
              <View
                key={itemIndex}
                className={styles.menuItem}
                onClick={() => {
                  if (item.url) {
                    Taro.navigateTo({ url: item.url });
                  } else {
                    Taro.showToast({ title: '功能开发中', icon: 'none' });
                  }
                }}
              >
                <Text className={styles.icon}>{item.icon}</Text>
                <Text className={styles.label}>{item.label}</Text>
                <Text className={styles.arrow}>›</Text>
              </View>
            ))}
          </View>
        ))}
      </View>
    </ScrollView>
  );
};

export default MinePage;
