# MergeMusicDesktop

> 聚合音乐 桌面端

## UI库的选取
JavaFX 自带的UI样式实在是有点过时，让我比较难以接受，于是就想着寻找一些其他的UI库轮子，正好在浏览[JavaFX 官方网站](https://openjfx.io/)时发现上面列出了一些社区轮子，由于我个人对 Google 的 Material Design 比较情有独钟，就看中了一个叫做 [MaterialFX](https://github.com/palexdev/MaterialFX) 的UI库。

在 GitHub 闲逛时，我又发现了另一个叫 [JFoenix](https://github.com/sshahine/JFoenix) 的库，拥有 `6k Star`，而且之前用过的一个软件 [HMCL](https://github.com/huanghongxun/HMCL) 就是用这个写的，然而当我尝试使用这个库时，却接连遇到问题。首先是其官网域名已经过期了，查阅文档比较麻烦，然后是在 Scene Builder 中很多组件显示异常……我翻阅仓库 issue ，发现也有很多人遇到类似的问题，其原因就是这个项目主要是针对`JDK8`开发的，新版本会有很多兼容性问题，最好的解决方法就是降版本....我最后还是选择浅尝辄止。

而 MaterialFX 诞生的其中一个目的就是为了取代老旧的 JFoenix ，虽然现在还在迭代过程中，有一些功能可能还不够稳定，但毕竟我所用到的功能应该也不会太过复杂，就决定是它了。

## 问题

### TableView 数据不同步
进行`MFXTableView<T>.setItems(ObservableList<T> items)`操作时，如果`items`初始时是一个空列表，那么后面的更改就无法被同步过去，原因不明。

解决方案为执行此命令前向`items`中传入一个`null`，之后立刻`clear`，之后再加时即可同步。

