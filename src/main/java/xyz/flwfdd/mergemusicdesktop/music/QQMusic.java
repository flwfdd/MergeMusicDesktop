package xyz.flwfdd.mergemusicdesktop.music;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import xyz.flwfdd.mergemusicdesktop.model.Config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/12 10:18
 * @implNote
 */

public class QQMusic extends Music {

    String id;
    String media_mid;

    QQMusic(Type type, String mid) {
        this.platform = Platform.QQ;
        this.type = type;
        this.mid = mid;
        id = mid.substring(1);
    }

    QQMusic(Type type, String mid, String name, List<String> artists, String albumName) {
        this(type, mid);
        this.name = name;
        this.artists = artists;
        this.albumName = albumName;
    }

    static String httpPost(String url1, String body) throws IOException {
        URL url = new URL(url1);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(11000);
        connection.setReadTimeout(11000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Referer", "y.qq.com");
        connection.setRequestProperty("Cookie", Config.getInstance().get("qq_music_cookie"));
        connection.setDoOutput(true);
        OutputStream outputStream = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        writer.write(body);
        writer.close();
        return new Scanner(connection.getInputStream()).useDelimiter("\\A").next();
    }

    static List<Music> search(String keyword, Type type, int limit, int page) {
        int typeCode = switch (type) {
            case LIST -> 3;
            case LYRIC -> 7;
            case USER -> 8;
            case ALBUM -> 2;
            default -> 0;
        };
        String url = "https://u.y.qq.com/cgi-bin/musicu.fcg";
        String body = """
                {
                    "music.search.SearchCgiService": {
                        "method": "DoSearchForQQMusicDesktop",
                        "module": "music.search.SearchCgiService",
                        "param": {
                            "num_per_page": %d,
                            "page_num": %d,
                            "query": "%s",
                            "search_type": %d
                        }
                    }
                }
                """;
        body = String.format(body, limit, page + 1, keyword, typeCode);
        try {
            String s = httpPost(url, body);
            JSONObject jsonBody = JSONObject.parseObject(s).getJSONObject("music.search.SearchCgiService")
                    .getJSONObject("data").getJSONObject("body");
            List<Music> musicList = new ArrayList<>();
            switch (type) {
                case LIST -> {
                    JSONArray list = jsonBody.getJSONObject("songlist").getJSONArray("list");
                    list.forEach(x -> {
                        var songList = (JSONObject) x;
                        musicList.add(new QQMusic(Type.LIST,
                                "Q" + songList.getString("dissid"),
                                songList.getString("dissname"),
                                List.of(songList.getJSONObject("creator").getString("name")),
                                ""
                        ));
                    });
                }
                case USER -> {
                    JSONArray list = jsonBody.getJSONObject("user").getJSONArray("list");
                    list.forEach(x -> {
                        var songList = (JSONObject) x;
                        musicList.add(new QQMusic(Type.USER,
                                "Q" + songList.getString("uin"),
                                songList.getString("title"),
                                List.of(),
                                ""
                        ));
                    });
                }
                case ALBUM -> {
                    JSONArray list = jsonBody.getJSONObject("album").getJSONArray("list");
                    list.forEach(x -> {
                        var album = (JSONObject) x;
                        musicList.add(new QQMusic(Type.ALBUM,
                                "Q" + album.getString("albumMID"),
                                album.getString("albumName"),
                                album.getJSONArray("singer_list").stream()
                                        .map(obj -> ((JSONObject) obj).getString("name")).collect(Collectors.toList()),
                                ""
                        ));
                    });
                }
                default -> {
                    JSONArray list = jsonBody.getJSONObject("song").getJSONArray("list");
                    list.forEach(x -> {
                        var song = (JSONObject) x;
                        musicList.add(new QQMusic(Type.MUSIC,
                                "Q" + song.getString("mid"),
                                song.getString("name"),
                                song.getJSONArray("singer").stream()
                                        .map(obj -> ((JSONObject) obj).getString("name")).collect(Collectors.toList()),
                                song.getJSONObject("album").getString("name")
                        ));
                    });
                }
            }

            return musicList;
        } catch (Exception e) {
            System.out.println("QQ search error:" + e);
        }
        return null;
    }

    void loadMusicInfo() throws IOException {
        String url = "https://u.y.qq.com/cgi-bin/musicu.fcg";
        String body = """
                {
                    "songinfo": {
                    "method": "get_song_detail_yqq",
                    "module": "music.pf_song_detail_svr",
                    "param": {
                        "song_mid": "%s"
                        }
                    }
                }
                """;
        body = String.format(body, id);
        String s = httpPost(url, body);
        JSONObject info = JSONObject.parseObject(s).getJSONObject("songinfo").getJSONObject("data").getJSONObject("track_info");
        name = info.getString("name");
        artists = info.getJSONArray("singer").stream()
                .map(obj -> ((JSONObject) obj).getString("name")).collect(Collectors.toList());
        albumName = info.getJSONObject("album").getString("name");
        img = String.format("https://y.gtimg.cn/music/photo_new/T002R500x500M000%s.jpg", info.getJSONObject("album").getString("mid"));
        media_mid = info.getJSONObject("file").getString("media_mid");
    }

    void loadMusicLrc() throws IOException {
        String url = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg";
        String body = "format=json&nobase64=1&g_tk=5381&songmid=" + id;
        String s = httpPost(url, body);
        JSONObject jsonObject = JSONObject.parseObject(s);
        lrc = jsonObject.getString("lyric");
        translateLrc = jsonObject.getString("trans");
    }

    void loadMusicSrc() throws IOException {
        String url = "https://u.y.qq.com/cgi-bin/musicu.fcg";
        String body = """
                {
                    "req_0":{
                        "module":"vkey.GetVkeyServer",
                        "method":"CgiGetVkey",
                        "param":{
                            "filename":[
                                "M500%s.mp3"
                            ],
                            "guid":"2333",
                            "songmid":[
                                "%s"
                            ],
                            "songtype":[
                                0
                            ],
                            "loginflag":1,
                            "platform":"20"
                        }
                    },
                    "comm":{
                        "format":"json",
                        "ct":24,
                        "cv":0
                    }
                }
                """;
        body = String.format(body, media_mid, id);
        String s = httpPost(url, body);
        JSONObject data = JSONObject.parseObject(s).getJSONObject("req_0").getJSONObject("data");
        String purl = data.getJSONArray("midurlinfo").getJSONObject(0).getString("purl");
        if (purl.isBlank()) src = "";
        else src = data.getJSONArray("sip").getString(0) + purl;
    }

    void loadMusic() {
        try {
            //加载基本信息
            loadMusicInfo();

            //加载歌词
            loadMusicLrc();

            //加载播放链接
            loadMusicSrc();

        } catch (Exception e) {
            System.out.println("load qq music error: " + e);
        }
    }

    @Override
    public void full_load() {
        if (type == Type.MUSIC) loadMusic();
        else throw new RuntimeException("Can only load music.");
    }

    List<Music> unfoldList() throws IOException {
        List<Music> musicList = new ArrayList<>();
        String url = "http://c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg";
        String body = "type=1&utf8=1&format=json&disstid=" + id;
        String s = httpPost(url, body);
        JSONArray list = JSONObject.parseObject(s).getJSONArray("cdlist").getJSONObject(0).getJSONArray("songlist");
        list.forEach(x -> {
            var song = (JSONObject) x;
            musicList.add(new QQMusic(Type.MUSIC,
                    "Q" + song.getString("songmid"),
                    song.getString("songname"),
                    song.getJSONArray("singer").stream()
                            .map(obj -> ((JSONObject) obj).getString("name")).collect(Collectors.toList()),
                    song.getString("albumname")
            ));
        });
        return musicList;
    }

    List<Music> unfoldUser() throws IOException {
        List<Music> musicList = new ArrayList<>();
        String url = "https://c.y.qq.com/rsc/fcgi-bin/fcg_user_created_diss";
        String body = "size=2333&inCharset=utf8&outCharset=utf8&hostuin=" + id;
        String s = httpPost(url, body);
        JSONArray list = JSONObject.parseObject(s).getJSONObject("data").getJSONArray("disslist");
        list.forEach(x -> {
            var diss = (JSONObject) x;
            if (!diss.getString("tid").equals("0")) musicList.add(new QQMusic(Type.LIST,
                    "Q" + diss.getString("tid"),
                    diss.getString("diss_name"),
                    List.of(name),
                    ""
            ));
        });
        return musicList;
    }

    List<Music> unfoldAlbum() throws IOException {
        String url = "https://u.y.qq.com/cgi-bin/musicu.fcg";
        String body = """
                {
                  "albumSonglist":{
                    "method":"GetAlbumSongList",
                    "param":{
                      "albumMid":"%s"
                    },
                    "module":"music.musichallAlbum.AlbumSongList"
                  }
                }
                """;
        body = String.format(body, id);
        String s = httpPost(url, body);
        JSONArray list = JSONObject.parseObject(s).getJSONObject("albumSonglist").getJSONObject("data").getJSONArray("songList");
        List<Music> musicList = new ArrayList<>();
        list.forEach(x -> {
            var song = ((JSONObject) x).getJSONObject("songInfo");
            musicList.add(new QQMusic(Type.MUSIC,
                    "Q" + song.getString("mid"),
                    song.getString("name"),
                    song.getJSONArray("singer").stream()
                            .map(obj -> ((JSONObject) obj).getString("name")).collect(Collectors.toList()),
                    song.getJSONObject("album").getString("name")
            ));
        });
        return musicList;
    }


    @Override
    List<Music> custom_unfold() {
        try {
            return switch (type) {
                case LIST -> unfoldList();
                case USER -> unfoldUser();
                case ALBUM -> unfoldAlbum();
                default -> null;
            };
        } catch (Exception e) {
            System.out.println("QQ music unfold error:" + e);
        }
        return null;
    }

    @Override
    String getLowImg() {
        return img;
    }
}
