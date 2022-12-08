package xyz.flwfdd.mergemusicdesktop.music;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import xyz.flwfdd.mergemusicdesktop.model.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/17 19:48
 * @implNote 网易云音乐模块
 */

class CloudMusic extends Music {
    String id;

    CloudMusic(Type type, String mid) {
        this.platform = Platform.CLOUD;
        this.type = type;
        this.mid = mid;
        id = mid.substring(1);
    }

    CloudMusic(Type type, String mid, String name, List<String> artists, String albumName) {
        this(type, mid);
        this.name = name;
        this.artists = artists;
        this.albumName = albumName;
    }

    static String httpGet(String url1) throws IOException {
        String apiUrl = Config.getInstance().get("cloud_music_api_url");
        String cookie = Config.getInstance().get("cloud_music_cookie");
        String url2 = apiUrl + url1 + "&realIP=114.246.205.187&cookie=" + URLEncoder.encode(cookie, StandardCharsets.UTF_8);
        URL url = new URL(url2);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(11000);
        connection.setReadTimeout(11000);
        connection.setRequestMethod("GET");
        return new Scanner(connection.getInputStream()).useDelimiter("\\A").next();
    }

    static Map<Type, String> type_map = new HashMap<>(Map.of(Type.MUSIC, "1", Type.LYRIC, "1006", Type.LIST, "1000", Type.USER, "1002"));

    static List<Music> search(String keyword, Type type, int limit, int page) {
        if (keyword.isEmpty()) return new ArrayList<>();
        String url = "/cloudsearch?keywords=%s&type=%s&limit=%d&offset=%d";
        if (!type_map.containsKey(type)) type = Type.MUSIC;
        url = String.format(url, URLEncoder.encode(keyword, StandardCharsets.UTF_8), type_map.get(type), limit, limit * page);
        try {
            String s = httpGet(url);
            JSONObject data = JSON.parseObject(s).getJSONObject("result");
            switch (type) {
                case MUSIC, LYRIC -> {
                    if (data.getIntValue("songCount") == 0) return new ArrayList<>();
                    return parseSongs(data.getJSONArray("songs"));
                }
                case LIST -> {
                    if (data.getIntValue("playlistCount") == 0) return new ArrayList<>();
                    List<Music> lists = new ArrayList<>();
                    data.getJSONArray("playlists").forEach(obj -> {
                        var song = (JSONObject) obj;
                        String artist = song.getJSONObject("creator").getString("nickname");
                        lists.add(new CloudMusic(Type.LIST,
                                "C" + song.getString("id"),
                                song.getString("name"),
                                Collections.singletonList(artist),
                                ""));
                    });
                    return lists;
                }
                case USER -> {
                    if (data.size() == 0) return new ArrayList<>();
                    List<Music> users = new ArrayList<>();
                    data.getJSONArray("userprofiles").forEach(obj -> {
                        var user = (JSONObject) obj;
                        users.add(new CloudMusic(Type.USER,
                                "C" + user.getString("userId"),
                                user.getString("nickname"),
                                Collections.singletonList(user.getString("nickname")),
                                ""));
                    });
                    return users;
                }
            }
        } catch (Exception e) {
            System.out.println("cloud music search error: " + e);
        }
        return null;
    }

    static List<Music> parseSongs(JSONArray songs) {
        List<Music> music_list = new ArrayList<>();
        songs.forEach(x -> {
            var song = (JSONObject) x;
            List<String> artists = song.getJSONArray("ar").stream()
                    .map(obj -> ((JSONObject) obj).getString("name")).collect(Collectors.toList());
            music_list.add(new CloudMusic(Type.MUSIC,
                    "C" + song.getString("id"),
                    song.getString("name"),
                    artists,
                    song.getJSONObject("al").getString("name")));
        });
        return music_list;
    }

    void loadMusic() {
        try {
            String s = httpGet("/song/detail/?ids=" + id);
            JSONObject data = JSON.parseObject(s).getJSONArray("songs").getJSONObject(0);
            name = data.getString("name");
            artists = data.getJSONArray("ar").stream()
                    .map(obj -> ((JSONObject) obj).getString("name")).collect(Collectors.toList());
            img = data.getJSONObject("al").getString("picUrl");
            albumName = data.getJSONObject("al").getString("name");

            s = httpGet("/lyric/?id=" + id);
            data = JSON.parseObject(s);
            lrc = data.getJSONObject("lrc").getString("lyric");
            if (data.containsKey("tlyric")) translateLrc = data.getJSONObject("tlyric").getString("lyric");

            s = httpGet("/song/url/?br=320000&id=" + id);
            data = JSON.parseObject(s);
            src = data.getJSONArray("data").getJSONObject(0).getString("url");
            if (src == null) throw new RuntimeException("Can't get src!");

        } catch (Exception e) {
            System.out.println("cloud music load music error: " + e);
        }
    }

    List<Music> loadUserList() throws IOException {
        // user->list 加载用户
        String s = httpGet("/user/playlist?uid=" + id);
        List<Music> lists = new ArrayList<>();
        JSON.parseObject(s).getJSONArray("playlist").forEach(obj -> {
            JSONObject l = (JSONObject) obj;
            lists.add(new CloudMusic(Type.LIST,
                    "C" + l.getString("id"),
                    l.getString("name"),
                    Collections.singletonList(l.getJSONObject("creator").getString("nickname")),
                    ""));
        });
        return lists;
    }

    List<Music> loadPlayList() throws IOException {
        // list->music 加载歌单
        String s = httpGet("/playlist/detail?id=" + id);
        String ids = JSON.parseObject(s).getJSONObject("playlist").getJSONArray("trackIds").stream()
                .map(obj -> ((JSONObject) obj).getString("id")).collect(Collectors.joining(","));
        s = httpGet("/song/detail/?ids=" + ids);
        return parseSongs(JSON.parseObject(s).getJSONArray("songs"));
    }

    public void custom_load() {
        if (type == Type.MUSIC) loadMusic();
        else throw new RuntimeException("Can only load music.");
    }

    public List<Music> custom_unfold() {
        try {
            if (type == Type.LIST) return loadPlayList();
            else if (type == Type.USER) return loadUserList();
            return null;
        } catch (Exception e) {
            System.out.println("cloud music load list error: " + e);
            return null;
        }
    }
}
