# MergeMusicDesktop

> 聚合音乐 桌面版

## UI库的选取
JavaFX 自带的UI样式实在是有点过时，让我比较难以接受，于是就想着寻找一些其他的UI库轮子，正好在浏览[JavaFX 官方网站](https://openjfx.io/)时发现上面列出了一些社区轮子，由于我个人对 Google 的 Material Design 比较情有独钟，就看中了一个叫做 [MaterialFX](https://github.com/palexdev/MaterialFX) 的UI库。

在 GitHub 闲逛时，我又发现了另一个叫 [JFoenix](https://github.com/sshahine/JFoenix) 的库，拥有 `6k Star`，而且之前用过的一个软件 [HMCL](https://github.com/huanghongxun/HMCL) 就是用这个写的，然而当我尝试使用这个库时，却接连遇到问题。首先是其官网域名已经过期了，查阅文档比较麻烦，然后是在 Scene Builder 中很多组件显示异常……我翻阅仓库 issue ，发现也有很多人遇到类似的问题，其原因就是这个项目主要是针对`JDK8`开发的，新版本会有很多兼容性问题，最好的解决方法就是降版本....我最后还是选择浅尝辄止。

而 MaterialFX 诞生的其中一个目的就是为了取代老旧的 JFoenix ，虽然现在还在迭代过程中，有一些功能可能还不够稳定，但毕竟我所用到的功能应该也不会太过复杂，就决定是它了。

## 音频播放方案
由于播放的格式可能为`.mp3`、`.m4a`、`.flac`等，但是 JavaSE 中的 Sound API 却只提供了基础的`.wav`支持，因此必须想其他办法。

首先我的想法集中于寻找第三方包，通过格式转换后再进行播放和处理。经过搜寻，有可以转换`aac`编码（也就是`.m4a`封装中的编码）的`jaadec`，也有可以解析`.mp3`的`jmp123`等，但是能同时实现各种格式转码的包很少，看起来非常强大的有`javacv`等封装了`ffmpeg`的包，但是`ffmpeg`本身并不能跨平台，虽然有一些取巧的解决方案，但是都会使得软件变得比较臃肿，得不偿失。

最后可谓是「众里寻他千百度，蓦然回首」，发现 Java FX 还有一个叫做 `javafx-media`的模块，几乎和浏览器中的`Web Audio API`别无二致（甚至我怀疑他们共享了同样的底层实现），查阅[文档](https://docs.oracle.com/javafx/2/api/javafx/scene/media/package-summary.html)发现可以满足需求（除了无法播放`.flac`，不过无损音乐的功能本来就不是特别常用，也可以通过再加一个包来解决），而且还可以直接使用链接播放，甚至还自带了频谱（都不用自己想办法做傅里叶分解了）！

## 问题

### TableView 数据不同步
进行`MFXTableView<T>.setItems(ObservableList<T> items)`操作时，如果`items`初始时是一个空列表，那么后面的更改就无法被同步过去，原因不明。

解决方案为执行此命令前向`items`中添加一个`null`，执行完绑定后立刻`clear`，之后再更改时即可同步。

### TableView 滚动错位
设置单元格鼠标悬浮提示时，有这样的代码
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

