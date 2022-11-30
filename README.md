# MergeMusicDesktop

> 聚合音乐 桌面版

## UI库的选取
JavaFX 自带的UI样式实在是有点过时，让我比较难以接受，于是就想着寻找一些其他的UI库轮子，正好在浏览[JavaFX 官方网站](https://openjfx.io/)时发现上面列出了一些社区轮子，由于我个人对 Google 的 Material Design 比较情有独钟，就看中了一个叫做 [MaterialFX](https://github.com/palexdev/MaterialFX) 的UI库。

在 GitHub 闲逛时，我又发现了另一个叫 [JFoenix](https://github.com/sshahine/JFoenix) 的库，拥有 `6k Star`，而且之前用过的一个软件 [HMCL](https://github.com/huanghongxun/HMCL) 就是用这个写的，然而当我尝试使用这个库时，却接连遇到问题。首先是其官网域名已经过期了，查阅文档比较麻烦，然后是在 Scene Builder 中很多组件显示异常……我翻阅仓库 issue ，发现也有很多人遇到类似的问题，其原因就是这个项目主要是针对`JDK8`开发的，新版本会有很多兼容性问题，最好的解决方法就是降版本....我最后还是选择浅尝辄止。

而 MaterialFX 诞生的其中一个目的就是为了取代老旧的 JFoenix ，虽然现在还在迭代过程中，有一些功能可能还不够稳定，但毕竟我所用到的功能应该也不会太过复杂，就决定是它了。

另外，由于 MaterialFX 的图标支持不是很好，又引入了 [Ikonli](https://github.com/kordamp/ikonli) 以及由谷歌维护的 [Material Icons](https://fonts.google.com/icons?selected=Material+Icons) 。

## 音频播放方案
由于播放的格式可能为`.mp3`、`.m4a`、`.flac`等，但是 JavaSE 中的 Sound API 却只提供了基础的`.wav`支持，因此必须想其他办法。

首先我的想法集中于寻找第三方包，通过格式转换后再进行播放和处理。经过搜寻，有可以转换`aac`编码（也就是`.m4a`封装中的编码）的`jaadec`，也有可以解析`.mp3`的`jmp123`等，但是能同时实现各种格式转码的包很少，看起来非常强大的有`javacv`等封装了`ffmpeg`的包，但是`ffmpeg`本身并不能跨平台，虽然有一些取巧的解决方案，但是都会使得软件变得比较臃肿，得不偿失。

最后可谓是「众里寻他千百度，蓦然回首」，发现 Java FX 还有一个叫做 `javafx-media`的模块，几乎和浏览器中的`Web Audio API`别无二致（甚至我怀疑他们共享了同样的底层实现），查阅[文档](https://docs.oracle.com/javafx/2/api/javafx/scene/media/package-summary.html)发现可以满足需求（除了无法播放`.flac`，不过无损音乐的功能本来就不是特别常用，也可以通过再加一个包来解决），而且还可以直接使用链接播放，甚至还自带了频谱（都不用自己想办法做傅里叶分解了）！

## 打包与分发
开始时尝试了 IntelliJ 的 artifacts 导出 JavaFX Application 的方案以及其他的一些 maven 插件，但是都遇到了各种各样的报错，最终发现`JDK`中的`jpackage`一行命令就能搞定....

```
jpackage --name MergeMusic --input .\MergeMusicDesktop_jar\ --vendor raven --main-jar .\MergeMusicDesktop.jar --type app-image
```

果然还是应该先把官方的东西吃透。感觉 Java 生态和之前比较了解的前端生态的一大不同之处就是 Java 的生态还是官方主导的，大部分会用到的东西官方都已经做好了，反而是社区的东西很多情况下不如官方的好使。这点前端生态就跟更加去中心化一些，

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
发现点击播放歌曲时会稍稍卡一下，但是获取歌曲的部分已经做了异步了，后来发现是因为执行`new Image(url)`初始化时不是异步的，造成了几百毫秒的阻塞，将图片加载部分扔到异步里就行了。

### 内存占用分析

偶然打开任务管理器发现竟然占了1G+的内存，于是尝试使用 Java 监视与管理控制台（JConsole）进行了监控，发现内存占用一直在锯齿状大波动。查资料又发现了一个叫做 JProfiler 的工具，可以进行比较详细的 Java 运行监控。发现是在加载歌曲时会有一个很大的内存暴涨，导致 JVM 申请了很大的内存，之后又一直都在反复占用，但是手动进行垃圾回收后内存占用会降低到100+M，并且可以稳定正常运行。后来尝试加上了运行参数`-Xmx256m`，仍然可以正常运行，应该不是代码的问题，只是单纯 JVM 内存分配机制的原因。