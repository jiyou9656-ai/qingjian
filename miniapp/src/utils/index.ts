import Taro from '@tarojs/taro';

export const formatDistance = (distance: string): string => {
  return distance;
};

export const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return `${(num / 10000).toFixed(1)}w`;
  }
  if (num >= 1000) {
    return `${(num / 1000).toFixed(1)}k`;
  }
  return String(num);
};

export const isLoggedIn = (): boolean => {
  return !!Taro.getStorageSync('token');
};

export const showToast = (title: string, icon: 'success' | 'error' | 'none' = 'none') => {
  Taro.showToast({ title, icon, duration: 2000 });
};
