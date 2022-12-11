package xyz.flwfdd.mergemusicdesktop.music;

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
        CLOUD;

        @Override
        public String toString() {
            return switch (this) {
                case CLOUD -> "网易云";
            };
        }

        public String getExtension() {
            return switch (this) {
                case CLOUD -> ".mp3";
            };
        }
    }

    public enum Type {
        MUSIC, LIST, USER, LYRIC;

        @Override
        public String toString() {
            return switch (this) {
                case MUSIC -> "歌曲";
                case LIST -> "列表";
                case USER -> "用户";
                case LYRIC -> "歌词";
            };
        }
    }

    static DB db = DB.getInstance();

    public static List<Music> search(String keyword, Platform platform, Type type, int limit, int page) {
        // page从0开始
        List<Music> music_list = null;
        if (platform == Platform.CLOUD) music_list = CloudMusic.search(keyword, type, limit, page);
        return music_list;
    }

    public static Music getMusic(String mid, String name, String lrc, String translateLrc, String albumName, List<String> artists) {
        Music music = switch (mid.charAt(0)) {
            case 'C' -> new CloudMusic(Type.MUSIC, mid);
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
            img=getLowImg();
            db.updateMusic(this);
            db.cacheMusic(this);
        } else {
            src = music.src;
            img = music.img;
        }
    }

    abstract List<Music> custom_unfold(); //展开音乐列表

    public List<Music> unfold() {
        List<Music> l = custom_unfold();
        l.forEach(music -> db.updateMusic(music));
        return l;
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

    @Override
    public String toString() {
        return String.format("%s(%s):%s", mid, type, name);
    }

}
