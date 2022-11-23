package xyz.flwfdd.mergemusicdesktop.music;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import xyz.flwfdd.mergemusicdesktop.model.Config;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/17 19:48
 * @implNote 网易云音乐模块
 */

class CloudMusic extends Music {
    static String getApiUrl(){
        return Config.getInstance().get("cloud_music_api_url");
    }

    static String getCookie(){
        return Config.getInstance().get("cloud_music_cookie");
    }

    CloudMusic(Type type, String mid) {
        this.type = type;
        this.mid = mid;
        id = mid.substring(1);
    }

    CloudMusic(Type type, String mid, String name, List<String> artists, String albumName) {
        this(type,mid);
        this.name = name;
        this.artists = artists;
        this.albumName = albumName;
    }

    static Map<Type, String> type_map = new HashMap<>(Map.of(Type.MUSIC, "1", Type.LYRIC, "1006", Type.LIST, "1000", Type.USER, "1002"));

    static List<Music> search(String keyword, Type type, int limit, int page) {
        if(keyword.isEmpty())return new ArrayList<>();
        String url = getApiUrl() + "/cloudsearch?keywords=%s&type=%s&limit=%d&offset=%d";
        if (!type_map.containsKey(type)) type = Type.MUSIC;
        url = String.format(url, keyword, type_map.get(type), limit, limit * page);
        try {
            String s = httpGet(url);
            JSONObject data = JSON.parseObject(s).getJSONObject("result");
            switch (type) {
                case MUSIC ,LYRIC -> {
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

    static String httpGet(String url) throws IOException {
        return Music.httpGet(url + "&realIP=114.246.205.187&cookie=" + getCookie());
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
            String s = httpGet(getApiUrl() + "/song/url/?id=" + id);
            JSONObject data = JSON.parseObject(s);
            src = data.getJSONArray("data").getJSONObject(0).getString("url");
            if (src == null) throw new RuntimeException("Can't get src!");

            s = httpGet(getApiUrl() + "/song/detail/?ids=" + id);
            data = JSON.parseObject(s).getJSONArray("songs").getJSONObject(0);
            name = data.getString("name");
            artists = data.getJSONArray("ar").stream()
                    .map(obj -> ((JSONObject) obj).getString("name")).collect(Collectors.toList());
            img = data.getJSONObject("al").getString("picUrl");
            albumName = data.getJSONObject("al").getString("name");

            s = Music.httpGet(getApiUrl() + "/lyric/?id=" + id);
            data = JSON.parseObject(s);
            lrc = data.getJSONObject("lrc").getString("lyric");
            if (data.containsKey("tlyric")) translateLrc = data.getJSONObject("tlyric").getString("lyric");
        } catch (Exception e) {
            System.out.println("cloud music load music error: " + e);
        }
    }

    List<Music> loadUserList() throws IOException {
        // user->list 加载用户
        String s = httpGet(getApiUrl() + "/user/playlist?uid=" + id);
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
        String s = httpGet(getApiUrl() + "/playlist/detail?id=" + id);
        String ids = JSON.parseObject(s).getJSONObject("playlist").getJSONArray("trackIds").stream()
                .map(obj -> ((JSONObject) obj).getString("id")).collect(Collectors.joining(","));
        s = httpGet(getApiUrl() + "/song/detail/?ids=" + ids);
        return parseSongs(JSON.parseObject(s).getJSONArray("songs"));
    }

    public void load() {
        if (type.equals(Type.MUSIC)) loadMusic();
        else throw new RuntimeException("Can only load music.");
    }

    public List<Music> unfold() {
        try {
            if (type.equals(Type.LIST)) return loadPlayList();
            else if (type.equals(Type.USER)) return loadUserList();
            return null;
        } catch (Exception e) {
            System.out.println("cloud music load list error: " + e);
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%s):%s", mid, type, name);
    }


}
