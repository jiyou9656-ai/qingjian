import Taro from '@tarojs/taro';

const BASE_URL = 'http://localhost:8080';

export const request = async <T>(url: string, options?: Taro.request.Option): Promise<T> => {
  try {
    const res = await Taro.request({
      url: `${BASE_URL}${url}`,
      method: 'GET',
      header: {
        'Content-Type': 'application/json',
        'token': Taro.getStorageSync('token'),
      },
      ...options,
    });

    if (res.statusCode === 200) {
      return res.data as T;
    } else {
      console.error('[API] Request failed:', res.statusCode, url);
      throw new Error(`Request failed: ${res.statusCode}`);
    }
  } catch (error) {
    console.error('[API] Request error:', error, url);
    throw error;
  }
};

export const api = {
  // 商户相关
  getShops: (params?: { type?: string; page?: number; size?: number }) =>
    request('/api/shop/list', { data: params }),

  getShopDetail: (id: number) =>
    request(`/api/shop/${id}`),

  // 笔记相关
  getBlogs: (params?: { page?: number; size?: number }) =>
    request('/api/blog/list', { data: params }),

  getBlogDetail: (id: number) =>
    request(`/api/blog/${id}`),

  likeBlog: (id: number) =>
    request(`/api/blog/like/${id}`, { method: 'POST' }),

  // 秒杀相关
  getSeckillVouchers: () =>
    request('/api/voucher/seckill/list'),

  seckillOrder: (voucherId: number) =>
    request(`/api/seckill/order/${voucherId}`, { method: 'POST' }),

  // 评价相关
  getComments: (shopId: number, params?: { page?: number; size?: number }) =>
    request(`/api/comment/${shopId}/list`, { data: params }),

  // 用户相关
  login: (phone: string, code: string) =>
    request('/api/user/login', {
      method: 'POST',
      data: { phone, code },
    }),
};
