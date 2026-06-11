export default defineAppConfig({
  pages: [
    'pages/home/index',
    'pages/discover/index',
    'pages/publish/index',
    'pages/message/index',
    'pages/mine/index',
    'pages/shop-detail/index',
    'pages/blog-detail/index',
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#00BFA5',
    navigationBarTitleText: '青荐',
    navigationBarTextStyle: 'white',
  },
  tabBar: {
    color: '#86909C',
    selectedColor: '#00BFA5',
    backgroundColor: '#FFFFFF',
    borderStyle: 'white',
    list: [
      {
        pagePath: 'pages/home/index',
        text: '首页',
      },
      {
        pagePath: 'pages/discover/index',
        text: '发现',
      },
      {
        pagePath: 'pages/publish/index',
        text: '发布',
      },
      {
        pagePath: 'pages/message/index',
        text: '消息',
      },
      {
        pagePath: 'pages/mine/index',
        text: '我的',
      },
    ],
  },
});
