import React from 'react';
import { View, Text, ScrollView, Image, Button } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import styles from './index.module.scss';
import { blogs } from '@/data/blogs';
import { formatNumber } from '@/utils';

const mockComments = [
  {
    id: 1,
    userName: '咖啡控',
    avatar: 'https://picsum.photos/id/91/200/200',
    content: '看起来好好吃！求地址',
  },
  {
    id: 2,
    userName: '探店小分队',
    avatar: 'https://picsum.photos/id/177/200/200',
    content: '收藏了，周末就去打卡',
  },
];

const BlogDetailPage: React.FC = () => {
  const router = useRouter();
  const blogId = Number(router.params.id);
  const blog = blogs.find((b) => b.id === blogId) || blogs[0];

  console.log('[BlogDetail] Blog id:', blogId);

  return (
    <ScrollView className={styles.blogDetailPage} scrollY>
      <View className={styles.header}>
        <View className={styles.userInfo}>
          <Image className={styles.avatar} src={blog.userAvatar} mode='aspectFill' />
          <View className={styles.info}>
            <Text className={styles.userName}>{blog.userName}</Text>
            <Text className={styles.createTime}>{blog.createTime}</Text>
          </View>
          <Button className={styles.followBtn}>+ 关注</Button>
        </View>
        <Text className={styles.title}>{blog.title}</Text>
        <Text className={styles.content}>{blog.content}</Text>
        <View className={styles.images}>
          {blog.images.map((img, index) => (
            <Image key={index} className={styles.image} src={img} mode='aspectFill' />
          ))}
        </View>
      </View>

      <View className={styles.actions}>
        <View className={`${styles.action} ${blog.liked ? styles.active : ''}`}>
          <Text className={styles.icon}>{blog.liked ? '❤️' : '🤍'}</Text>
          <Text className={styles.count}>{formatNumber(blog.likeCount)}</Text>
        </View>
        <View className={styles.action}>
          <Text className={styles.icon}>💬</Text>
          <Text className={styles.count}>{formatNumber(blog.commentCount)}</Text>
        </View>
        <View className={styles.action}>
          <Text className={styles.icon}>⭐</Text>
          <Text className={styles.count}>收藏</Text>
        </View>
        <View className={styles.action}>
          <Text className={styles.icon}>↗️</Text>
          <Text className={styles.count}>分享</Text>
        </View>
      </View>

      <View className={styles.comments}>
        <Text className={styles.title}>评论 ({mockComments.length})</Text>
        {mockComments.map((comment) => (
          <View key={comment.id} className={styles.commentItem}>
            <View className={styles.header}>
              <Image className={styles.avatar} src={comment.avatar} mode='aspectFill' />
              <Text className={styles.userName}>{comment.userName}</Text>
            </View>
            <Text className={styles.content}>{comment.content}</Text>
          </View>
        ))}
      </View>
    </ScrollView>
  );
};

export default BlogDetailPage;
