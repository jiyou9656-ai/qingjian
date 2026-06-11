import React, { useState } from 'react';
import { View, Text, Textarea, Input, Button } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';
import { showToast, isLoggedIn } from '@/utils';

const PublishPage: React.FC = () => {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  console.log('[Publish] Page loaded');

  const handleSubmit = () => {
    if (!isLoggedIn()) {
      showToast('请先登录', 'error');
      return;
    }

    if (!title.trim()) {
      showToast('请输入标题', 'error');
      return;
    }

    if (!content.trim()) {
      showToast('请输入内容', 'error');
      return;
    }

    console.log('[Publish] Submit blog:', { title, content });
    showToast('发布成功', 'success');
    Taro.navigateBack();
  };

  const handleChooseImage = () => {
    console.log('[Publish] Choose image');
    Taro.chooseImage({
      count: 9,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        console.log('[Publish] Images selected:', res.tempFilePaths);
      },
    });
  };

  return (
    <View className={styles.publishPage}>
      <View className={styles.form}>
        <View className={styles.titleInput}>
          <Input
            className={styles.input}
            placeholder='输入标题'
            value={title}
            onInput={(e) => setTitle(e.detail.value)}
            maxlength={50}
          />
        </View>
        <View className={styles.contentInput}>
          <Textarea
            className={styles.textarea}
            placeholder='分享你的探店体验...'
            value={content}
            onInput={(e) => setContent(e.detail.value)}
            maxlength={1000}
          />
        </View>
        <View className={styles.imageUpload}>
          <View className={styles.uploadBtn} onClick={handleChooseImage}>
            <Text>📷</Text>
          </View>
        </View>
        <Button className={styles.submitBtn} onClick={handleSubmit}>
          发布
        </Button>
      </View>
    </View>
  );
};

export default PublishPage;
