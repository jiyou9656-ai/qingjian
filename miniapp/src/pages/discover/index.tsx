import React, { useState } from 'react';
import { View, Text, ScrollView } from '@tarojs/components';
import styles from './index.module.scss';
import BlogCard from '@/components/BlogCard';
import { blogs } from '@/data/blogs';

const DiscoverPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'recommend' | 'follow'>('recommend');

  console.log('[Discover] Page loaded');

  return (
    <ScrollView className={styles.discoverPage} scrollY>
      <View className={styles.header}>
        <View className={styles.tabs}>
          <Text
            className={`${styles.tab} ${activeTab === 'recommend' ? styles.active : ''}`}
            onClick={() => setActiveTab('recommend')}
          >
            推荐
          </Text>
          <Text
            className={`${styles.tab} ${activeTab === 'follow' ? styles.active : ''}`}
            onClick={() => setActiveTab('follow')}
          >
            关注
          </Text>
        </View>
      </View>
      <View className={styles.blogList}>
        {blogs.map((blog) => (
          <BlogCard key={blog.id} blog={blog} />
        ))}
      </View>
    </ScrollView>
  );
};

export default DiscoverPage;
