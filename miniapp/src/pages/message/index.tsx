import React from 'react';
import { View, Text, ScrollView } from '@tarojs/components';
import styles from './index.module.scss';
import { Message } from '@/types';

const mockMessages: Message[] = [
  {
    id: 1,
    type: 'like',
    title: '点赞通知',
    content: '美食达人 赞了你的笔记「这家茶餐厅的菠萝油绝了！」',
    createTime: '10分钟前',
    isRead: false,
  },
  {
    id: 2,
    type: 'comment',
    title: '评论通知',
    content: '咖啡控 评论了你的笔记：「请问这家店具体在哪里呀？」',
    createTime: '30分钟前',
    isRead: false,
  },
  {
    id: 3,
    type: 'follow',
    title: '新关注',
    content: '探店小分队 关注了你',
    createTime: '2小时前',
    isRead: true,
  },
  {
    id: 4,
    type: 'system',
    title: '系统通知',
    content: '你参与的「海底捞火锅」秒杀活动已成功，请在有效期内使用优惠券。',
    createTime: '1天前',
    isRead: true,
  },
  {
    id: 5,
    type: 'like',
    title: '点赞通知',
    content: '奶茶星人 赞了你的笔记「喜茶新品多肉葡萄测评」',
    createTime: '2天前',
    isRead: true,
  },
];

const getMessageIcon = (type: string): string => {
  switch (type) {
    case 'like':
      return '❤️';
    case 'comment':
      return '💬';
    case 'follow':
      return '👤';
    case 'system':
      return '🔔';
    default:
      return '📩';
  }
};

const MessagePage: React.FC = () => {
  console.log('[Message] Page loaded');

  return (
    <ScrollView className={styles.messagePage} scrollY>
      {mockMessages.length > 0 ? (
        <View className={styles.messageList}>
          {mockMessages.map((msg) => (
            <View key={msg.id} className={`${styles.messageItem} ${!msg.isRead ? styles.unread : ''}`}>
              <View className={styles.icon}>{getMessageIcon(msg.type)}</View>
              <View className={styles.content}>
                <Text className={styles.title}>{msg.title}</Text>
                <Text className={styles.desc}>{msg.content}</Text>
                <Text className={styles.time}>{msg.createTime}</Text>
              </View>
              {!msg.isRead && <View className={styles.unreadDot} />}
            </View>
          ))}
        </View>
      ) : (
        <View className={styles.empty}>
          <Text className={styles.icon}>📭</Text>
          <Text className={styles.text}>暂无消息</Text>
        </View>
      )}
    </ScrollView>
  );
};

export default MessagePage;
