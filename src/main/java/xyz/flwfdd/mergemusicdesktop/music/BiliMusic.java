package xyz.flwfdd.mergemusicdesktop.music;

import com.alibaba.fastjson2.JSONObject;
import xyz.flwfdd.mergemusicdesktop.model.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/13 11:15
 * @implNote
 */

public class BiliMusic extends Music {

    String aid;
    String cid;
    boolean isUp=false;
    boolean isSupportPartUnfold=false;

    BiliMusic(Type type, String mid) {
        this.platform = Platform.BILI;
        this.type = type;
        this.mid = mid;
        aid = mid.substring(1);
        if(type==Type.MUSIC)aid=aid.substring(0,aid.lastIndexOf('_'));
    }

    BiliMusic(Type type, String mid, String name, List<String> artists, String albumName) {
        this(type, mid);
        this.name = name;
        this.artists = artists;
        this.albumName = albumName;
    }

    BiliMusic(String mid, String cid, String name, List<String> artists, String albumName, String img) {
        // 构造分P
        this(Type.MUSIC,mid);
        this.cid = cid;
        this.name = name;
        this.artists = artists;
        this.albumName = albumName;
        this.img = img;
    }

    static String httpGet(String url1) throws IOException {
        String cookie = Config.getInstance().get("bili_cookie");
        if (cookie.isBlank()) cookie = "buvid3=" + new Random().nextInt();
        URL url = new URL(url1);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(11000);
        connection.setReadTimeout(11000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", cookie);
        connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36 Edg/107.0.1418.62");
        return new Scanner(connection.getInputStream()).useDelimiter("\\A").next();
    }

    static String titleDecode(String s){
        return s.replaceAll("<em class=\"keyword\">", "")
                .replaceAll("</em>", "")
                .replaceAll("&amp;","&")
                .replaceAll("&lt;","<")
                .replaceAll("&gt;",">")
                .replaceAll("&quot","\"");
    }

    static List<Music> search(String keyword, Type type, int limit, int page) {
        String searchType = type == Type.USER ? "bili_user" : "video";
        String url = "https://api.bilibili.com/x/web-interface/search/type?keyword=%s&search_type=%s&page=%d&page_size=%d";
        url = String.format(url, URLEncoder.encode(keyword, StandardCharsets.UTF_8), searchType, page + 1, limit);
        try {
            String s = httpGet(url);
            JSONObject data = JSONObject.parseObject(s).getJSONObject("data");
            if (page >= data.getIntValue("numPages")) return List.of();
            List<Music> musicList = new ArrayList<>();
            data.getJSONArray("result").forEach(obj -> {
                var m = (JSONObject) obj;
                if (searchType.equals("video")) {
                    musicList.add(new BiliMusic(Type.LIST,
                            "B" + m.getString("aid"),
                            titleDecode(m.getString("title")),
                            List.of(m.getString("author")),
                            ""
                    ));
                } else {
                    var bm=new BiliMusic(Type.USER,
                            "B" + m.getString("mid"),
                            m.getString("uname"),
                            List.of(m.getString("usign")),
                            ""
                    );
                    bm.isUp=(m.getIntValue("is_upuser")==1);
                    bm.isSupportPartUnfold=true;
                    musicList.add(bm);
                }
            });
            return musicList;
        } catch (IOException e) {
            System.out.println("bili search error:" + e);
        }
        return null;
    }

    @Override
    public void full_load() {
        if (type == Type.MUSIC) {
            try {
                if(cid==null){
                    int page=Integer.parseInt(mid.substring(mid.indexOf('_')+1));
                    BiliMusic m=(BiliMusic) new BiliMusic(Type.LIST,"B"+aid).unfold().get(page-1);
                    cid=m.cid;
                    img=m.img;
                    name=m.name;
                    artists=m.artists;
                    albumName=m.albumName;
                }
                String url = "https://api.bilibili.com/x/player/playurl?fnval=80&avid=%s&cid=%s";
                url = String.format(url, aid, cid);
                String s = httpGet(url);
                AtomicInteger maxBandWidth = new AtomicInteger();
                JSONObject.parseObject(s).getJSONObject("data").getJSONObject("dash").getJSONArray("audio")
                        .forEach(obj -> {
                            var audio = (JSONObject) obj;
                            if (audio.getIntValue("bandwidth") > maxBandWidth.get()) {
                                maxBandWidth.set(audio.getIntValue("bandwidth"));
                                src=audio.getString("base_url");
                            }
                        });
            } catch (Exception e) {
                System.out.println("bili full load error:" + e);
            }
        } else throw new RuntimeException("Can only load music.");
    }


    boolean isFavList=false;
    boolean isUpVideos=false;
    int unfoldPage=1;
    List<Music> unfoldUser() throws IOException {
        List<Music> musicList = new ArrayList<>();
        if(isFavList){ //收藏夹而非用户
            if(isUpVideos){ //加载UP作品
                String url="https://api.bilibili.com/x/space/arc/search?ps=50&mid=%s&pn=%s";
                url=String.format(url,aid,unfoldPage);
                String s=httpGet(url);
                JSONObject.parseObject(s).getJSONObject("data").getJSONObject("list").getJSONArray("vlist")
                        .forEach(obj->{
                            var v=(JSONObject)obj;
                            musicList.add(new BiliMusic(Type.LIST,
                                    "B"+v.getString("aid"),
                                    v.getString("title"),
                                    List.of(v.getString("author")),
                                    ""
                                ));
                        });
            } else {
                String url="https://api.bilibili.com/x/v3/fav/resource/list?ps=20&media_id=%s&pn=%s";
                url=String.format(url,aid,unfoldPage);
                String s=httpGet(url);
                JSONObject data=JSONObject.parseObject(s).getJSONObject("data");
                if(data.getJSONArray("medias")==null)return List.of();
                data.getJSONArray("medias")
                        .forEach(obj->{
                            var v=(JSONObject)obj;
                            musicList.add(new BiliMusic(Type.LIST,
                                    "B"+v.getString("id"),
                                    v.getString("title"),
                                    List.of(v.getJSONObject("upper").getString("name")),
                                    ""
                            ));
                        });
            }
        } else { //用户
            if(unfoldPage==1&&isUp){
                var bm=new BiliMusic(Type.USER,"B"+aid,"UP投稿",List.of(name),"");
                bm.isUpVideos=true;
                bm.isFavList=true;
                bm.isSupportPartUnfold=true;
                musicList.add(bm);
            }
            String url="https://api.bilibili.com/x/v3/fav/folder/created/list?pn=%s&ps=100&up_mid=%s";
            url=String.format(url,unfoldPage,aid);
            String s=httpGet(url);
            JSONObject data=JSONObject.parseObject(s).getJSONObject("data");
            if(data==null)return musicList;
            data.getJSONArray("list").forEach(obj->{
                var fav=(JSONObject)obj;
                var bm=new BiliMusic(Type.USER,
                        "B"+fav.getString("id"),
                        fav.getString("title"),
                        List.of(name),
                        ""
                        );
                bm.isFavList=true;
                bm.isSupportPartUnfold=true;
                musicList.add(bm);
            });
        }
        unfoldPage++;
        return musicList;
    }

    List<Music> unfoldVideo() throws IOException {
        String url = "https://api.bilibili.com/x/web-interface/view?aid=" + aid;
        String s = httpGet(url);
        List<Music> musicList = new ArrayList<>();
        JSONObject video = JSONObject.parseObject(s).getJSONObject("data");
        video.getJSONArray("pages").forEach(obj -> {
            var p = (JSONObject) obj;
            musicList.add(new BiliMusic(
                    "B" + video.getString("aid") + "_" + p.getString("page"),
                    p.getString("cid"),
                    video.getString("title"),
                    video.containsKey("staff")?video.getJSONArray("staff").stream()
                            .map(o -> ((JSONObject) o).getString("name")).collect(Collectors.toList())
                        :List.of(video.getJSONObject("owner").getString("name")),
                    p.getString("part"),
                    video.getString("pic")
            ));
        });
        return musicList;
    }

    @Override
    List<Music> custom_unfold() {
        try {
            return switch (type) {
                case USER -> unfoldUser();
                case LIST -> unfoldVideo();
                default -> null;
            };
        } catch (Exception e) {
            System.out.println("bili unfold error:" + e);
        }
        return null;
    }

    @Override
    String getLowImg() {
        return img+"@424w_424h";
    }

    @Override
    public Map<String, String> getHeaders() {
        return Map.of("Referer","https://api.bilibili.com");
    }

    @Override
    public boolean supportPartUnfold(){
        return isSupportPartUnfold;
    }

    @Override
    public void resetUnfoldPage(){
        unfoldPage=1;
    }
}
