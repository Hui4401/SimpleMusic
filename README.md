# SimpleMusic

Android期末项目，仅用作学习使用，在线音乐部分只获取了网易云热歌榜，API来自 [鼻子亲了脸](https://www.bzqll.com/)

<img src="https://cdn.jsdelivr.net/gh/Hui4401/imgbed/project/SimpleMusic/4.jpg" width = "22%"/> <img src="https://cdn.jsdelivr.net/gh/Hui4401/imgbed/project/SimpleMusic/1.png" width = "22%"/> <img src="https://cdn.jsdelivr.net/gh/Hui4401/imgbed/project/SimpleMusic/2.png" width = "22%"/> <img src="https://cdn.jsdelivr.net/gh/Hui4401/imgbed/project/SimpleMusic/3.png" width = "22%"/>

## 1. 开发环境
- AndroidStudio 3.2
- AndroidSDK 5.0
- gradle 4.6

## 2. 模块划分

| 模块 | 简要说明 |
|--|--|
| 后台服务(Service) | 提供音乐播放的基本功能, 对外提供播放控制和列表操作的接口 |
| 主界面 | 展示我的音乐歌单, 可以对歌单里的音乐进行播放, 删除等操作 |
| 本地音乐 | 展示本地音乐列表, 提供对本地音乐的读取, 并且可以播放, 添加收藏, 删除 |
| 在线音乐 | 展示当前热门歌曲, 提供播放，添加收藏，删除等功能，需要时可以刷新列表 |
| 播放器界面 | 展示当前音乐的信息, 控制播放模式, 上一曲, 播放/暂停, 下一曲，以及查看和操作当前播放列表 |

## 3. 实现功能
1. 展示出本地的所有音乐文件，并显示相关信息
2. 对本地音乐进行播放, 添加歌单, 删除等操作
3. 播放过程中，可以暂停播放、恢复播放、上一首、下一首、拖动音乐进度条到任意时间点播放
4. 播放时，能显示当前播放音乐的进度，指示播放的时间, 并加载专辑图片
5. 可以设置播放的顺序为顺序播放、单曲循环、随机播放等等
6. 音乐播放器等界面退出以后，音乐仍然能在后台播放
7. 每次启动音乐播放器，播放器装载之前的播放列表，并把列表中的第一首音乐作为默认的首播曲
8. 再次打开播放器，能显示当前音乐的实时信息
9. 可以连接到网络，播放网络上的音乐
10. 缓存网络音乐列表, 需要时刷新, 也可以添加歌单或者移除
11. 音频焦点控制, 被其他音频打断后能继续播放

## 4. 简要说明

### 各java文件的作用
| 文件名 | 功能 |
|--|--|
| /java/db/*.java | litepal采用对象映射操作数据库, 每个类对应一张表 |
| /java/view/RotateAnimator.java | 唱盘动画制作 |
| /java/bean/Music.java | 音乐类 |
| /java/adapter/*.java | 适配器类 |
| /java/activity/*.java | 所有活动 |
| /java/service/MusicService.java | 音乐服务 |
| /java/util/Utils.java | 工具类, 封装了一些常用操作 |

### 各xml文件的作用
| 文件名 | 功能 |
|--|--|
| /res/anim/*.xml | Activity切换动画 |
| /res/layout/activity_*.xml | 界面布局 |
| /res/layout/music_item.xml | 音乐列表项布局 |
| /res/layout/playinglist_item.xml | 播放列表项布局 |
| /res/layout/layout_discview.xml | 播放器唱盘布局 |
| /res/layout/nav_header.xml | 侧拉菜单头布局 |
| /res/menu/main.xml | 主界面活动跳转菜单布局 |
| /res/menu/nav_menu.xml | 侧拉菜单项布局 |
