package xyz.flwfdd.mergemusicdesktop.music;

import org.junit.jupiter.api.Test;

class BiliMusicTest {

    @Test
    public void testBiliSearch(){
        var l=Music.search("范滇东", Music.Platform.BILI, Music.Type.MUSIC,4,0);
        System.out.println(l);
        l=Music.search("范滇东", Music.Platform.BILI, Music.Type.MUSIC,2,1);
        System.out.println(l);
        l=Music.search("范滇东", Music.Platform.BILI, Music.Type.USER,4,0);
        System.out.println(l);
        l=Music.search("范滇东", Music.Platform.BILI, Music.Type.USER,4,2333);
        System.out.println(l);
    }

    @Test
    public void testUnfoldVideo(){
        var l=new BiliMusic(Music.Type.LIST,"B419034237");
        System.out.println(l.unfold());
        l=new BiliMusic(Music.Type.LIST,"B41421409");
        System.out.println(l.unfold());
    }

    @Test
    public void testUnfoldUser(){
        var u=new BiliMusic(Music.Type.USER,"B27482524");
        u.isUp=true;
        var l=u.unfold();
        System.out.println(l);
        System.out.println(l.get(0).unfold());
        var ll=l.get(2).unfold();
        System.out.println(ll);
        ll=l.get(2).unfold();
        System.out.println(ll);
    }

    @Test
    public void testFullLoad(){
        var l=new BiliMusic(Music.Type.LIST,"B41421409");
        var m=l.unfold().get(0);
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

}