export interface Shop {
  id: number;
  name: string;
  type: string;
  images: string[];
  address: string;
  distance: string;
  score: number;
  comments: number;
  avgPrice: number;
  isSeckill?: boolean;
}

export interface Blog {
  id: number;
  userId: number;
  userName: string;
  userAvatar: string;
  title: string;
  content: string;
  images: string[];
  liked: boolean;
  likeCount: number;
  commentCount: number;
  createTime: string;
}

export interface Comment {
  id: number;
  userId: number;
  userName: string;
  userAvatar: string;
  score: number;
  content: string;
  createTime: string;
}

export interface Voucher {
  id: number;
  shopId: number;
  shopName: string;
  title: string;
  originalPrice: number;
  payValue: number;
  stock: number;
  beginTime: string;
  endTime: string;
}

export interface Message {
  id: number;
  type: 'system' | 'like' | 'comment' | 'follow';
  title: string;
  content: string;
  createTime: string;
  isRead: boolean;
}

export interface User {
  id: number;
  nickName: string;
  avatar: string;
  phone: string;
}
