import React from 'react';
import { View, Text, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';
import { Blog } from '@/types';
import { formatNumber } from '@/utils';

interface BlogCardProps {
  blog: Blog;
}

const BlogCard: React.FC<BlogCardProps> = ({ blog }) => {
  const handleNavigate = () => {
    console.log('[BlogCard] Navigate to blog detail:', blog.id);
    Taro.navigateTo({
      url: `/pages/blog-detail/index?id=${blog.id}`,
    });
  };

  const handleLike = (e: any) => {
    e.stopPropagation();
    console.log('[BlogCard] Like blog:', blog.id);
  };

  const displayImages = blog.images.slice(0, 3);

  return (
    <View className={styles.blogCard} onClick={handleNavigate}>
      <View className={styles.header}>
        <Image className={styles.avatar} src={blog.userAvatar} mode='aspectFill' />
        <View className={styles.userInfo}>
          <Text className={styles.userName}>{blog.userName}</Text>
          <Text className={styles.createTime}>{blog.createTime}</Text>
        </View>
      </View>
      <Text className={styles.title}>{blog.title}</Text>
      <Text className={styles.content}>{blog.content}</Text>
      {displayImages.length > 0 && (
        <View className={styles.images}>
          {displayImages.map((img, index) => (
            <Image key={index} className={styles.image} src={img} mode='aspectFill' />
          ))}
        </View>
      )}
      <View className={styles.footer}>
        <View className={`${styles.action} ${blog.liked ? styles.liked : ''}`} onClick={handleLike}>
          <Text className={styles.icon}>{blog.liked ? '❤️' : '🤍'}</Text>
          <Text className={styles.count}>{formatNumber(blog.likeCount)}</Text>
        </View>
        <View className={styles.action}>
          <Text className={styles.icon}>💬</Text>
          <Text className={styles.count}>{formatNumber(blog.commentCount)}</Text>
        </View>
      </View>
    </View>
  );
};

export default BlogCard;
