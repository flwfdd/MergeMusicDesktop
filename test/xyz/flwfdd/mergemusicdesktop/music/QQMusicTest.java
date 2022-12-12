package xyz.flwfdd.mergemusicdesktop.music;

import org.junit.jupiter.api.Test;

import java.util.List;

class QQMusicTest {

    @Test
    public void testQQMusic() {
        Music m = new QQMusic(Music.Type.MUSIC,"Q0003wCkl4OrELF");
        m.full_load();
        System.out.println(m.getMid());
        System.out.println(m.getName());
        System.out.println(m.getAlbumName());
        System.out.println(m.getArtists());
        System.out.println(m.getImg());
        System.out.println(m.getSrc());
        System.out.println(m.getLrc());
        System.out.println(m.getTranslateLrc());
    }

    @Test
    public void testQQListUnfold() {
        Music m = new QQMusic(Music.Type.LIST,"Q7479057129");
        System.out.println(m.unfold());
    }

    @Test
    public void testQQUserUnfold() {
        Music m = new QQMusic(Music.Type.USER,"QoK4A7Kcl7wSioz**");
        System.out.println(m.unfold());
        System.out.println(m.unfold().get(0).unfold());
    }

    @Test
    public void testQQSearchMusic(){
        List<Music> l = Music.search("阶", Music.Platform.QQ, Music.Type.MUSIC, 4, 0);
        System.out.println(l);
        System.out.println(l.get(0).getMid());
        System.out.println(l.get(0).getName());
        System.out.println(l.get(0).getArtists());
        System.out.println(l.get(0).getAlbumName());
        l = Music.search("", Music.Platform.QQ, Music.Type.MUSIC, 4, 0);
        System.out.println(l);
        l = Music.search("&", Music.Platform.QQ, Music.Type.MUSIC, 4, 0);
        System.out.println(l);
        l = Music.search("夜航星", Music.Platform.QQ, Music.Type.MUSIC, 2, 1);
        System.out.println(l);
        l = Music.search("夜航星", Music.Platform.QQ, Music.Type.MUSIC, 24, 0);
        System.out.println(l.size());
    }

    @Test
    public void testQQSearchList(){
        List<Music> l = Music.search("我的三体", Music.Platform.QQ, Music.Type.LIST, 4, 0);
        System.out.println(l);
    }

    @Test
    public void testQQSearchUser(){
        List<Music> l = Music.search("海の声音", Music.Platform.QQ, Music.Type.USER, 4, 0);
        System.out.println(l);
        System.out.println(l.get(1).unfold());
    }

    @Test
    public void testQQSearchLyrics(){
        List<Music> l = Music.search("明明拥着黑夜的双肩", Music.Platform.QQ, Music.Type.LYRIC, 4, 0);
        System.out.println(l);
    }

}