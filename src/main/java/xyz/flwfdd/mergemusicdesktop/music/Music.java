package xyz.flwfdd.mergemusicdesktop.music;

import java.util.List;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/17 19:44
 * @implNote 音乐API主类 所有外部操作都只调用这个 不同平台实现子类
 */

public abstract class Music {
    public enum Platform{
        CLOUD;

        @Override
        public String toString() {
            return switch (this){
                case CLOUD -> "网易云";
            };
        }
    }

    public enum Type{
        MUSIC,LIST,USER,LYRIC;

        @Override
        public String toString() {
            return switch (this){
                case MUSIC -> "歌曲";
                case LIST -> "列表";
                case USER -> "用户";
                case LYRIC -> "歌词";
            };
        }
    }

    public static List<Music> search(String keyword, Platform platform, Type type, int limit, int page) {
        // page从0开始
        List<Music> music_list = null;
        if (platform.equals(Platform.CLOUD)) music_list = CloudMusic.search(keyword, type, limit, page);
        return music_list;
    }

    public static Music getMusic(String mid) {
        try {
            Music music;
            if (mid.startsWith("C")) music=new CloudMusic(Type.MUSIC, mid);
            else return null;
            music.load();
            return music;
        } catch (Exception e) {
            System.out.println("get music error: " + e);
            return null;
        }
    }

    public abstract void load(); //加载音乐详细信息、封面图、播放链接等

    public abstract List<Music> unfold(); //展开音乐列表

    String mid, id, name="", src="", img="", lrc="", translateLrc="", albumName="";
    Type type;
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

    public Type getType(){
        return type;
    }

    @Override
    public String toString() {
        return String.format("%s(%s):%s", mid, type, name);
    }

}
