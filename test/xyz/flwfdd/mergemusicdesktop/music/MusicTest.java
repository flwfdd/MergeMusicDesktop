package xyz.flwfdd.mergemusicdesktop.music;

import org.junit.jupiter.api.Test;


/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/17 20:03
 * 音乐模块测试
 */
class MusicTest {
    @Test
    public void testCloudUser() {
        var users = Music.search("范滇东", Music.Platform.CLOUD, Music.Type.USER, 24, 0);
        System.out.println(users.size());
        System.out.println(users);
        var lists = users.get(0).loadList();
        System.out.println(lists.size());
        System.out.println(lists);
        var musics = lists.get(0).loadList();
        System.out.println(musics.size());
        System.out.println(musics);
        var m = musics.get(5);
        m.load();
        System.out.println("Src "+m.getSrc());
        System.out.println("Img "+m.getImg());
    }

    @Test
    public void testCloudMusic() {
        Music m = Music.getMusic("C554245242");
        assert m != null;
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
    public void testCloudSearchMusic() {
        var l = Music.search("wocaikendingsoubudao", Music.Platform.CLOUD, Music.Type.MUSIC, 4, 0);
        System.out.println(l);
        l = Music.search("", Music.Platform.CLOUD, Music.Type.MUSIC, 4, 0);
        System.out.println(l);
        l = Music.search("赤羽", Music.Platform.CLOUD, Music.Type.MUSIC, 4, 0);
        System.out.println(l);
        l = Music.search("赤羽", Music.Platform.CLOUD, Music.Type.MUSIC, 2, 1);
        System.out.println(l);
    }
}