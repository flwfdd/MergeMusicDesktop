# MergeMusicDesktop

> 聚合音乐 桌面端

## UI库的选取
JavaFX 自带的UI样式实在是有点过时，让我比较难以接受，于是就想着寻找一些其他的UI库轮子，正好在浏览[JavaFX 官方网站](https://openjfx.io/)时发现上面列出了一些社区轮子，由于我个人对 Google 的 Material Design 比较情有独钟，就看中了一个叫做 [MaterialFX](https://github.com/palexdev/MaterialFX) 的UI库。

在 GitHub 闲逛时，我又发现了另一个叫 [JFoenix](https://github.com/sshahine/JFoenix) 的库，拥有 `6k Star`，而且之前用过的一个软件 [HMCL](https://github.com/huanghongxun/HMCL) 就是用这个写的，然而当我尝试使用这个库时，却接连遇到问题。首先是其官网域名已经过期了，查阅文档比较麻烦，然后是在 Scene Builder 中很多组件显示异常……我翻阅仓库 issue ，发现也有很多人遇到类似的问题，其原因就是这个项目主要是针对`JDK8`开发的，新版本会有很多兼容性问题，最好的解决方法就是降版本....我最后还是选择浅尝辄止。

而 MaterialFX 诞生的其中一个目的就是为了取代老旧的 JFoenix ，虽然现在还在迭代过程中，有一些功能可能还不够稳定，但毕竟我所用到的功能应该也不会太过复杂，就决定是它了。

另外，由于 MaterialFX 的图标支持不是很好，又引入了 [Ikonli](https://github.com/kordamp/ikonli) 以及由谷歌维护的 [Material Icons](https://fonts.google.com/icons?selected=Material+Icons) 。

## 音频播放方案
由于播放的格式可能为`.mp3`、`.m4a`、`.flac`等，但是 JavaSE 中的 Sound API 却只提供了基础的`.wav`支持，因此必须想其他办法。

首先我的想法集中于寻找第三方包，通过格式转换后再进行播放和处理。经过搜寻，有可以转换`aac`编码（也就是`.m4a`封装中的编码）的`jaadec`，也有可以解析`.mp3`的`jmp123`等，但是能同时实现各种格式转码的包很少，看起来非常强大的有`javacv`等封装了`ffmpeg`的包，但是`ffmpeg`本身并不能跨平台，虽然有一些取巧的解决方案，但是都会使得软件变得比较臃肿，得不偿失。

最后可谓是「众里寻他千百度，蓦然回首」，发现 Java FX 还有一个叫做 `javafx-media`的模块，几乎和浏览器中的`Web Audio API`别无二致（甚至我怀疑他们共享了同样的底层实现），查阅[文档](https://docs.oracle.com/javafx/2/api/javafx/scene/media/package-summary.html)发现可以满足需求（除了无法播放`.flac`，不过无损音乐的功能本来就不是特别常用，也可以通过再加一个包来解决），而且还可以直接使用链接播放，甚至还自带了频谱（都不用自己想办法做傅里叶分解了）！

## 音乐API部分

### QQ音乐

参考项目：[QQMusicApi](https://github.com/jsososo/QQMusicApi)

#### 搜索
`POST https://u.y.qq.com/cgi-bin/musicu.fcg`

`body`为一个`JSON`格式的字符串，格式如下：
```json
{
    "music.search.SearchCgiService": {
        "method": "DoSearchForQQMusicDesktop",
        "module": "music.search.SearchCgiService",
        "param": {
            "num_per_page": 24, //每页条目数
            "page_num": 1, //页码 从1开始
            "query": "夜航星", //关键词 无需编码
            "search_type": 0 //0为歌曲 2为专辑 3为歌单 7为歌词 8为用户
        }
    }
}
```

#### 获取歌曲详情
`POST https://u.y.qq.com/cgi-bin/musicu.fcg`

`body`为一个`JSON`格式的字符串，格式如下：
```json
{
  "songinfo": {
    "method": "get_song_detail_yqq",
    "module": "music.pf_song_detail_svr",
    "param": {
      "song_mid": "004QtMmf2AeGHZ" //填入歌曲mid
    }
  }
}
```

#### 获取歌词
`POST https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg`

`body`格式为`format=json&nobase64=1&g_tk=5381&songmid=004QtMmf2AeGHZ`，注意该接口要求添加`Headers`：`Referer:y.qq.com`。


#### 获取歌曲播放链接
`POST https://u.y.qq.com/cgi-bin/musicu.fcg`

`body`为一个`JSON`格式的字符串，格式如下：
```json
{
  "req_0": {
    "module": "vkey.GetVkeyServer",
    "method": "CgiGetVkey",
    "param": {
      "filename":["M500002I2lcO3lqZFW.mp3"], //不传则默认为m4a格式
      "guid": "2333",
      "songmid": [
        "004QtMmf2AeGHZ" //填入mid
      ],
      "songtype": [
        0
      ],
      "loginflag": 1,
      "platform": "20"
    }
  },
  "comm": {
    "format": "json",
    "ct": 24,
    "cv": 0
  }
}
```
其中`filename`的计算需要用到`media_mid`，在歌曲详细信息接口中可以获取到。

#### 获取歌单详情
`POST http://c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg`

`body`格式为`type=1&utf8=1&format=json&disstid=7479057129`，需要`Referer`。

#### 获取用户歌单
`POST https://c.y.qq.com/rsc/fcgi-bin/fcg_user_created_diss`

`body`格式为`size=2333&inCharset=utf8&outCharset=utf8&hostuin=1234567890`

#### 获取专辑歌曲
`POST https://u.y.qq.com/cgi-bin/musicu.fcg`

`body`为一个`JSON`格式的字符串，格式如下：
```json
{
  "albumSonglist":{
    "method":"GetAlbumSongList",
    "param":{
      "albumMid":"003lkdBY4bs97f" //填入专辑mid
    },
    "module":"music.musichallAlbum.AlbumSongList"
  }
}
```


## 数据库
使用`sqlite-jdbc`操作`SQLite`数据库。

开始设计时，考虑到列表和歌曲是一个多对多的关系，所以就想到先对歌曲和列表分别建立一张表，然后再通过一张中间表记录对应的`list_id`和`music_id`来将两张表关联起来。但是后来发现每次对于列表的操作都是整读整取，不存在读部分列表的情况，所以就没有必要建立三张表了，直接把一个列表里的歌曲列成一个字符串，然后存到一个字段里就行。

## 打包与分发
开始时尝试了 IntelliJ 的 Artifacts 导出 JavaFX Application 的方案以及其他的一些 maven 插件，但是都遇到了各种各样的报错，最终发现`JDK`中的`jpackage`一行命令就能搞定....

可以参考文档 [Packaging Tool User's Guide](https://docs.oracle.com/en/java/javase/19/jpackage/index.html) 和 [The jpackage Command](https://docs.oracle.com/en/java/javase/19/docs/specs/man/jpackage.html) 。

果然还是应该先把官方的东西吃透。感觉 Java 生态和之前比较了解的前端生态的一大不同之处就是 Java 的生态还是官方主导的，大部分会用到的东西官方都已经做好了，反而是社区的东西很多情况下不如官方的好使。这点前端生态就跟更加去中心化一些。

整个流程为：
1. 克隆项目
2. 加载`pom.xml`中指定的 maven 依赖
3. 执行`mvn clean package`
4. 删除`target`目录中除了打包完整的`.jar`之外的所有文件（否则会被打包进去）
5. 在项目根目录下运行如下命令（可根据需求修改）
```shell
jpackage --name MergeMusicDesktop --input .\target\ --main-jar .\MergeMusicDesktop-1.0-SNAPSHOT.jar --type app-image --icon .\other\launcher.ico --resource-dir .\other\ --app-version 0.0.0.0 --copyright "Copyright flwfdd All Rights Reserved" --description MergeMusicDesktop-聚合音乐桌面端
```


## 问题

### TableView 数据不同步
进行`MFXTableView<T>.setItems(ObservableList<T> items)`操作时，如果`items`初始时是一个空列表，那么后面的更改就无法被同步过去，原因不明。

解决方案为执行此命令前向`items`中添加一个`null`，执行完绑定后立刻`clear`，之后再更改时即可同步。


### TableView 滚动错位
设置单元格鼠标悬浮提示时，有这样的代码：
```java
colName.setRowCellFactory(music->{
    var rowCell=new MFXTableRowCell<>(Music::getName);
    new MFXTooltip(rowCell,music.getName()).install();
    return rowCell;
});
```
但是测试时发现，在表格滚动后，悬浮的提示内容不会随着滚动更改，也就是这里的`music`变量其实并不是每一行真正的`music`，真正的是通过传给`MFXTableRowCell`的那个函数动态设置的。

最后通过把悬浮提示文字绑定到单元格文字上解决。
```java
void createTooltip(MFXTableRowCell<Music,String> rowCell){
    var tooltip=new MFXTooltip(rowCell);
    tooltip.textProperty().bind(rowCell.textProperty());
    tooltip.install();
}
```


### 按键事件鬼畜
实现空格切换播放暂停、方向键调节音量和播放进度等功能后，测试时发现空格偶尔会触发切换歌曲的操作，感到非常不解。后来发现是如果先点击了下一首按钮，再按空格就会触发下一首按钮的事件，应该是当焦点在按钮上时，按下空格就相当于按下按钮。

开始时空格事件是通过在根`Scene`上添加`addEventHandler`实现的，改成了`addEventFilter`并且立即`consume`后，事件就不会再派发下去了，解决了这个问题。

然而另一个关于输入框的问题又浮现出来。尽管通过实验发现输入框的文字输入以及`setOnKeyPressed`（用于监听回车搜索）和`addEvent`的事件并不在同一套系统里，即使在上层消费掉了事件，也不会影响输入框的输入。但是当输入框输入空格时，同样会触发播放暂停事件（从操作逻辑上这显然是不合理的），而且这个问题无法通过在输入框中消费事件解决，因为父级会先捕获到事件。

这个问题的解决方案是在捕获事件时对事件的`target`进行一系列的判断，如果是输入框则不执行操作。

万万没想到，又一个问题出现了：如果焦点在一个按钮上时按下空格执行了播放暂停的操作，那么之后这个按钮就会对鼠标点击无响应，需要到焦点移开时才能够继续响应点击事件。猜测是`onAction`本来是对空格事件有相应的，但是事件被消费造成某个环节卡住了。

解决方法堪称奇葩——在`consume`事件的同时，让一个无关紧要的组件（实际代码中使用了展示背景图片的`backgroundPane`）来`requestFocus`以把焦点移走，这样焦点不在原来的按钮上，自然也就没有问题了。


### 条件 Bingding 没有更新
实现音量控制模块时，想要做一个一键静音功能，而最终的音量在没有静音时与音量条一致，静音时则设置为0。开始时我这样写：
```java
realVolume=new When(mute).then(showVolume).otherwise(0);
```
却发现静音按钮可以使用，但是当拖动进度条也就是改变`showVolume`时，`realVolume`并没有更新，看来是只会绑定到条件语句上。最后更换为这样的写法就可以了：
```java
realVolume=showVolume.multiply(new When(mute).then(0).otherwise(1));
```


### Image 类异步加载
发现点击播放歌曲时会稍稍卡一下，但是获取歌曲的部分已经做了异步了，后来发现是因为执行`new Image(url)`初始化时不是异步的，造成了几百毫秒的阻塞，将图片加载部分扔到另一个线程里就行了。


### 内存占用分析
偶然打开任务管理器发现竟然占了1G+的内存，于是尝试使用 Java 监视与管理控制台（JConsole）进行了监控，发现内存占用一直在锯齿状大波动。查资料又发现了一个叫做 JProfiler 的工具，可以进行比较详细的 Java 运行监控。发现是在加载歌曲时会有一个很大的内存暴涨，导致 JVM 申请了很大的内存，之后又一直都在反复占用，但是手动进行垃圾回收后内存占用会降低到`100+MB`，并且可以稳定正常运行。后来尝试加上了运行参数`-Xmx256m`，仍然可以正常运行， JProfiler 里也能看到内存占用一直没有超出过`256MB`，但是任务管理器中内存占用仍然会到`800+MB`，我感到非常不解。

经过测试发现当播放音乐时内存就会暴涨，进一步调试发现真正造成这个现象的不是播放音乐，而是加载歌曲图片。加载一个`10MB`左右的图片就会使得内存暴涨几百兆，这应该是 JavaFX 中`Image`类实现的问题，其内部没有用压缩方式存储图片，且内存释放逻辑也有问题。我没能想到怎么从正面角度解决这个问题，只能减小图片尺寸，避免使用`Image`加载大尺寸图片。


### URL 编码
开始使用了现成的`OkHttp`库，后来发现并没有太大必要，想换回`URLConnection`时，发生了错误。
```shell
java.io.IOException: Server returned HTTP response code: 400 for URL
```
原因就是`URLConnection`并不会对链接进行自动转义，如果链接中包含了中文或空格等字符就会出错，需要手动使用`URLEncoder.encode`进行转义。但后来又发现如果把整个`url`都进行转义，那么包括`http://`中的符号等也会一并转义，还是不行，所以就只能对其中可能包含非法字符的部分进行转义。


### 数据库多线程冲突
当多个线程同时调用数据库时，会出现错误：
```shell
org.sqlite.SQLiteException: [SQLITE_BUSY] The database file is locked (database is locked)
```
解决方法是把获取数据库连接的代码单独为一个函数，并且用`synchronized`关键字修饰，这样当有冲突时后面的就会等待前面的连接释放。
```java
synchronized Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbURL);
    }
```

### 数据库`REPLACE INTO`语句问题
本来使用这个语句来进行更新条目，如果不存在就新建。后来发现这条语句的更新并非是真正更新，而是替换。举例来说，我有一个`src`字段设置了默认值，当执行`REPLACE INTO`命令时，虽然没有更新`src`字段，但是这个字段并没有保持原来的值而是变成了默认值。

解决方法是改成了`INSERT INTO .... ON CONFLICT DO UPDATE SET`的形式。

### 打包jar运行报错问题
打包为单个`.jar`文件运行时出现了错误：
```shell
Caused by: java.lang.UnsupportedOperationException: Cannot resolve 'mdrmz-skip_previous'
        at org.kordamp.ikonli.AbstractIkonResolver.resolve(AbstractIkonResolver.java:61)
        at org.kordamp.ikonli.javafx.IkonResolver.resolve(IkonResolver.java:73)
        at org.kordamp.ikonli.javafx.FontIcon.setIconLiteral(FontIcon.java:251)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
        ... 23 more
```
看样子是 ikonli 图标库的问题，于是到项目的 issue 下搜索，发现项目官网就提供了[解决方法](https://kordamp.org/ikonli/#_creating_a_fat_jar)，只需要在 maven 里添加一个插件即可。具体的原理还没有研究，反正是能用了。

在此之后也就不能使用 IntelliJ 的 Artifacts 来进行打包了，而要使用 maven 命令`mvn clean package`。

打包完运行又出现错误提示“没有主清单属性”，还需要在插件配置里加上以下内容来指定主类。
```xml
<transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
    <mainClass>xyz.flwfdd.mergemusicdesktop.Main</mainClass>
</transformer>
```

### 点击关闭后没有完全退出
点击关闭按钮关闭程序后，IntelliJ 中仍然显示程序没有停止，开始还以为是 IntelliJ 卡了，后来使用 JProfiler 监控发现是有一个线程一直没有退出。窗口关闭按钮只会强制退出主线程，如果要完全退出程序，需要加入如下代码：
```java
stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
    @Override
    public void handle(WindowEvent windowEvent) {
        System.exit(0);
    }
});
```
这样确实关闭的时候就完全退出了。但是这是治标不治本的办法，之前是没有这个问题的，肯定是加了某些代码后造成了这个问题，所以决定排查出来。然而调试工具并不能指出那个没有退出的线程是由哪里的代码产生的，我把所有`new Thread()`的地方排查了一遍也没有找到问题所在。最终通过不停回退`git`版本，发现是在加入了提示消息的时候引入了这个问题，进一步排查发现是为了实现提示消息一定时间自动隐藏的功能，定义了一个对象的全局变量`Timer timer = new Timer();`，如果把这个`Timer`改为即用即抛的局部变量就不会出现问题，推测是`Timer`会保持一个线程，而因为定义在对象的属性里，不会被自动回收，就一直不退出了。

