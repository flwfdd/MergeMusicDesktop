package xyz.flwfdd.mergemusicdesktop.music;

import javafx.beans.property.SimpleDoubleProperty;
import xyz.flwfdd.mergemusicdesktop.DownloadController;
import xyz.flwfdd.mergemusicdesktop.model.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/17 19:44
 * @implNote 音乐API主类 所有外部操作都只调用这个 不同平台实现子类
 */

public abstract class Music {
    public enum Platform {
        CLOUD, QQ, BILI;

        @Override
        public String toString() {
            return switch (this) {
                case CLOUD -> "网易云";
                case QQ -> "QQ";
                case BILI -> "B站";
            };
        }

        public String getExtension() {
            return switch (this) {
                case CLOUD, QQ -> ".mp3";
                case BILI -> ".m4a";
            };
        }

        public String getColor() {
            return switch (this) {
                case CLOUD -> "#A8001C";
                case QQ -> "#00A85F";
                case BILI -> "#FB7299";
            };
        }
    }

    public enum Type {
        MUSIC, LIST, USER, ALBUM, LYRIC;

        @Override
        public String toString() {
            return switch (this) {
                case MUSIC -> "歌曲";
                case LIST -> "歌单";
                case USER -> "用户";
                case ALBUM -> "专辑";
                case LYRIC -> "歌词";
            };
        }
    }

    static DB db = DB.getInstance();

    public static List<Music> search(String keyword, Platform platform, Type type, int limit, int page) {
        // page从0开始
        return switch (platform) {
            case CLOUD -> CloudMusic.search(keyword, type, limit, page);
            case QQ -> QQMusic.search(keyword, type, limit, page);
            case BILI -> BiliMusic.search(keyword, type, limit, page);
        };
    }

    public static Music getMusic(String mid, String name, String lrc, String translateLrc, String albumName, List<String> artists) {
        Music music = switch (mid.charAt(0)) {
            case 'C' -> new CloudMusic(Type.MUSIC, mid);
            case 'Q' -> new QQMusic(Type.MUSIC, mid);
            case 'B' -> new BiliMusic(Type.MUSIC, mid);
            default -> null;
        };
        if (music == null) return null;
        music.mid = mid;
        music.name = name;
        music.lrc = lrc;
        music.translateLrc = translateLrc;
        music.albumName = albumName;
        music.artists = artists;
        return music;
    }

    public static Music getMusic(String mid, String name, String lrc, String translateLrc, String albumName, List<String> artists, String src, String img) {
        Music music = getMusic(mid, name, lrc, translateLrc, albumName, artists);
        if (music == null) return null;
        music.src = src;
        music.img = img;
        return music;
    }

    public abstract void full_load(); //加载音乐播放链接、图片链接、歌词、专辑

    public void load() {
        Music music = db.getCacheMusic(mid);
        if (music == null || music.src.isBlank() || music.img.isBlank()) {
            System.out.println("Load:" + this);
            full_load();
            if (src == null) src = "";
            if (img == null) img = "";
            if (lrc == null) lrc = "";
            if (translateLrc == null) translateLrc = "";
            if (albumName == null) albumName = "";
            if (platform == Platform.BILI) src = Music.db.cacheMusicSrc(this);
            img = getLowImg();
            db.updateMusic(this);
            db.cacheMusic(this);
        } else {
            src = music.src;
            img = music.img;
            lrc = music.lrc;
            translateLrc = music.translateLrc;
        }
    }

    abstract List<Music> custom_unfold(); //展开音乐列表

    public List<Music> unfold() {
        List<Music> l = custom_unfold();
        if (l == null) return null;
        l.forEach(music -> db.updateMusic(music));
        return l;
    }

    public void downloadImg(Path path) {
        if (type != Type.MUSIC) {
            System.out.println("can only download music!");
            return;
        }
        Path path1 = Paths.get(path.toString(), db.getImgFileName(this));
        if (path1.toFile().exists()) Config.getInstance().setMsg("已经存在图片 " + name);
        else new Thread(() -> {
            SimpleDoubleProperty progress = DownloadController.createDownloadItem(name, path.toFile(), path1.toString());
            Config.getInstance().setMsg("开始下载图片 " + name);
            full_load();
            if (db.download(img, path1.toString(), getHeaders(), progress))
                Config.getInstance().setMsg("下载成功图片 " + name);
            else Config.getInstance().setMsg("下载失败图片 " + name);
        }).start();
    }

    public void downloadMusic(Path path) {
        if (type != Type.MUSIC) {
            System.out.println("can only download music!");
            return;
        }
        Path path1 = Paths.get(path.toString(), db.getMusicFileName(this));
        if (path1.toFile().exists()) Config.getInstance().setMsg("已经存在音乐 " + name);
        else new Thread(() -> {
            SimpleDoubleProperty progress = DownloadController.createDownloadItem(name, path.toFile(), path1.toString());
            Config.getInstance().setMsg("开始下载音乐 " + name);
            Music music = db.getCacheMusic(mid);
            if (music == null || music.src.isBlank()) full_load();
            if (db.download(src, path1.toString(), getHeaders(), progress))
                Config.getInstance().setMsg("下载成功音乐 " + name);
            else Config.getInstance().setMsg("下载失败音乐 " + name);
        }).start();
    }

    String mid, name = "", src = "", img = "", lrc = "", translateLrc = "", albumName = "";
    Type type;
    Platform platform;
    List<String> artists;

    public String getMid() { //获取mid mid第一个字母为平台
        return mid;
    }

    public String getName() { //获取名称
        return name;
    }

    public String getSrc() { //获取播放链接
        return src;
    }

    public String getImg() { //获取图片链接
        return img;
    }

    abstract String getLowImg(); //获取压缩图片链接

    public String getLrc() { //获取lrc格式歌词
        return lrc;
    }

    public String getTranslateLrc() { //获取lrc格式翻译歌词
        return translateLrc;
    }

    public String getAlbumName() { //获取专辑名
        return albumName;
    }

    public List<String> getArtists() { //获取作者名列表
        return artists;
    }

    public Type getType() {
        return type;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>();
    }

    public boolean supportPartUnfold() {
        return false;
    }

    public void resetUnfoldPage() {
    }

    public String getShare() {
        return "http://mergemusic.cn/#/?mid=" + mid;
    }

    @Override
    public String toString() {
        return String.format("%s(%s):%s", mid, type, name);
    }

}
