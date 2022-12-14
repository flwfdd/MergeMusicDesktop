package xyz.flwfdd.mergemusicdesktop.music;

import com.alibaba.fastjson2.JSON;
import javafx.beans.property.SimpleDoubleProperty;
import xyz.flwfdd.mergemusicdesktop.model.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/6 16:13
 * @implNote 数据库和缓存
 */

public class DB {

    static DB instance;
    String dbURL;
    String cachePath;
    long cacheSizeSum=0;

    synchronized Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbURL);
    }

    void initDB() {
        String dbPath = Paths.get(Config.getInstance().getRootPath(), "music.sqlite").toString();
        cachePath = Paths.get(Config.getInstance().getRootPath(), "cache").toString();
        File file = new File(dbPath);
        File cacheFile = new File(cachePath);
        try {
            if (!file.exists() && !file.createNewFile()) System.out.println("create database file error");
            if (!cacheFile.exists() && !cacheFile.mkdirs()) System.out.println("create cache dir error");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dbURL = "jdbc:sqlite:" + dbPath;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            String sql;
            //音乐
            sql = """
                    CREATE TABLE IF NOT EXISTS music(
                       mid           TEXT    PRIMARY KEY,
                       name          TEXT    NOT NULL,
                       lrc           TEXT    NOT NULL,
                       translate_lrc TEXT    NOT NULL,
                       album_name    TEXT    NOT NULL,
                       artists       TEXT    NOT NULL,
                       file_name     TEXT    NOT NULL DEFAULT '',
                       img_file_name TEXT    NOT NULL DEFAULT '',
                       refresh_time  INTEGER NOT NULL
                    );""";
            statement.execute(sql);

            //列表
            sql = """
                    CREATE TABLE IF NOT EXISTS list(
                       id     INTEGER PRIMARY KEY AUTOINCREMENT,
                       name   TEXT NOT NULL,
                       musics TEXT NOT NULL
                    );""";
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void init(){
        initDB();
        initCacheSize();
    }

    public static DB getInstance() {
        if (instance == null){
            instance = new DB();
            instance.init();
        }
        return instance;
    }

    String list2String(List<String> l) {
        return JSON.toJSONString(l);
    }

    List<String> string2List(String s) {
        return JSON.parseArray(s, String.class);
    }

    List<Music> selectMusics(List<String> midList) {
        // 从数据库中抽取出歌曲列表 不包含源
        Map<String, Music> map = new HashMap<>();
        String sql = "SELECT * FROM music WHERE mid in (" +
                midList.stream().map(x -> "?").collect(Collectors.joining(",")) + ");";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < midList.size(); i++) statement.setString(i + 1, midList.get(i));
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("mid"), Music.getMusic(
                        rs.getString("mid"),
                        rs.getString("name"),
                        rs.getString("lrc"),
                        rs.getString("translate_lrc"),
                        rs.getString("album_name"),
                        string2List(rs.getString("artists"))));
            }
            return midList.stream().map(map::get).toList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateMusic(Music music) {
        // 更新数据库
        if (music.getType() != Music.Type.MUSIC) return;
        String sql = """
                INSERT INTO music (mid,name,lrc,translate_lrc,album_name,artists,refresh_time)
                VALUES (?,?,?,?,?,?,?)
                ON CONFLICT DO UPDATE SET mid=?,name=?,lrc=?,translate_lrc=?,album_name=?,artists=?,refresh_time=?;
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, music.getMid());
            statement.setString(2, music.getName());
            statement.setString(3, music.getLrc());
            statement.setString(4, music.getTranslateLrc());
            statement.setString(5, music.getAlbumName());
            statement.setString(6, list2String(music.getArtists()));
            statement.setInt(7, (int) (System.currentTimeMillis() / 1000));
            statement.setString(8, music.getMid());
            statement.setString(9, music.getName());
            statement.setString(10, music.getLrc());
            statement.setString(11, music.getTranslateLrc());
            statement.setString(12, music.getAlbumName());
            statement.setString(13, list2String(music.getArtists()));
            statement.setInt(14, (int) (System.currentTimeMillis() / 1000));
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer,String> getLists(){
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT id,name FROM list WHERE id!=1");
            Map<Integer,String> map=new HashMap<>();
            while (rs.next()) {
                map.put(rs.getInt("id"),rs.getString("name"));
            }
            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Music> getList(int id){
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT * FROM list WHERE id="+id);
            if (rs.next()) {
                List<String> l = string2List(rs.getString("musics"));
                return selectMusics(l);
            } else return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int createList(String name){
        String sql = "INSERT INTO list (name,musics) VALUES (?,'[]')";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1,name);
            statement.execute();
            var rs=statement.getGeneratedKeys();
            if(rs.next())return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public void setList(int id,List<Music> musicList){
        String sql = "UPDATE list SET musics=? WHERE id=?;";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, list2String(musicList.stream().map(Music::getMid).toList()));
            statement.setInt(2, id);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteList(int id){
        if(id==1){
            System.out.println("can not delete playlist");
            return;
        }
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM list WHERE id="+id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 以下为缓存部分

    Pattern illegalFileNamePattern = Pattern.compile("[\\\\/:*?\"<>|\r\n]"); //操作系统非法字符

    String getFileName(Music music) {
        //生成音乐文件名
        String s = music.getName() + " - " + String.join(",", music.getArtists()) + " - " + music.getMid();
        Matcher matcher = illegalFileNamePattern.matcher(s);
        return matcher.replaceAll("");
    }

    String getMusicFileName(Music music) {
        return getFileName(music) + music.getPlatform().getExtension();
    }

    String getImgFileName(Music music) {
        return getFileName(music) + ".jpg";
    }

    final static int buffSize = 102400; //下载片大小

    boolean download(String src, String path, Map<String, String> headers) {
        return download(src,path,headers,new SimpleDoubleProperty());
    }

    boolean download(String src, String path, Map<String, String> headers, SimpleDoubleProperty progress) {
        File file = Paths.get(path).toFile();
        try {
            if(src.startsWith("file")){
                Files.copy(Paths.get(URI.create(src)),file.toPath());
                progress.set(1);
            } else {
                URL url = new URL(src);
                URLConnection con = url.openConnection();
                headers.forEach(con::setRequestProperty);
                InputStream is = con.getInputStream();
                int totalLen=con.getContentLength();
                int sumLen=0;
                byte[] bs = new byte[buffSize];
                FileOutputStream os = new FileOutputStream(file);
                int len;
                while ((len = is.read(bs)) != -1) {
                    os.write(bs, 0, len);
                    sumLen+=len;
                    progress.set(((double)sumLen)/totalLen);
                }
                os.close();
                is.close();
            }
        } catch (Exception e) {
            System.out.println("Download error:" + e);
            if(!file.delete())System.out.println("download delete fail: "+file.getPath());
            return false;
        }
        return true;
    }

    String cacheMusicSrc(Music music){
        // 同步缓存音乐 返回缓存链接
        if (download(music.getSrc(), Paths.get(cachePath, getMusicFileName(music)).toString(), music.getHeaders())) {
            updateCacheSize();
            String sql = "UPDATE music SET file_name=? WHERE mid=?;";
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, getMusicFileName(music));
                statement.setString(2, music.getMid());
                statement.execute();
                cacheSizeSum+=Paths.get(cachePath, getMusicFileName(music)).toFile().length();
                return Paths.get(cachePath, getMusicFileName(music)).toUri().toString();
            } catch (SQLException e) {
                Config.getInstance().setMsg("缓存音乐失败："+music.getName());
                System.out.println("Cache src error:" + music);
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    void cacheMusic(Music music) {
        // 异步缓存音乐
        if (music.getSrc() != null && !music.getSrc().isBlank() && !music.getSrc().startsWith("file")) {
            new Thread(() -> cacheMusicSrc(music)).start();
        }
        if (music.getImg() != null && !music.getImg().isBlank() && !music.getImg().startsWith("file")) {
            new Thread(() -> {
                if (download(music.getImg(), Paths.get(cachePath, getImgFileName(music)).toString(), music.getHeaders())) {
                    String sql = "UPDATE music SET img_file_name=? WHERE mid=?;";
                    try (Connection connection = getConnection();
                         PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, getImgFileName(music));
                        statement.setString(2, music.getMid());
                        statement.execute();
                        cacheSizeSum+=Paths.get(cachePath, getImgFileName(music)).toFile().length();
                    } catch (SQLException e) {
                        Config.getInstance().setMsg("缓存图片失败："+music.getName());
                        System.out.println("Cache image error:" + music);
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    Music getCacheMusic(String mid) {
        Music music = null;
        // 获取缓存的歌曲
        String sql = "SELECT * FROM music WHERE mid=?;";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, mid);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                String src = "", img = "";
                Path filePath = Paths.get(cachePath, rs.getString("file_name"));
                if (filePath.toFile().isFile() && filePath.toFile().exists()) src = filePath.toUri().toString();
                filePath = Paths.get(cachePath, rs.getString("img_file_name"));
                if (filePath.toFile().isFile() && filePath.toFile().exists()) img = filePath.toUri().toString();
                music = Music.getMusic(
                        rs.getString("mid"),
                        rs.getString("name"),
                        rs.getString("lrc"),
                        rs.getString("translate_lrc"),
                        rs.getString("album_name"),
                        string2List(rs.getString("artists")),
                        src,
                        img);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (music != null) updateMusic(music);
        return music;
    }

    void initCacheSize(){
        // 初始化扫描缓存大小
        List<String> noFileMusics=new ArrayList<>(); //需要取消音乐文件缓存记录的音乐mid列表
        List<String> noImgMusics=new ArrayList<>(); //需要取消图片文件缓存记录的音乐mid列表
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT * FROM music WHERE file_name!='' OR img_file_name!=''");
            while (rs.next()) {
                Path filePath = Paths.get(cachePath, rs.getString("file_name"));
                if (filePath.toFile().isFile() && filePath.toFile().exists()){
                    cacheSizeSum+=filePath.toFile().length();
                } else noFileMusics.add(rs.getString("mid"));
                filePath = Paths.get(cachePath, rs.getString("img_file_name"));
                if (filePath.toFile().isFile() && filePath.toFile().exists()){
                    cacheSizeSum+=filePath.toFile().length();
                } else noImgMusics.add(rs.getString("mid"));
            }
            statement.execute("UPDATE music SET file_name='' WHERE mid in ('" +String.join("','",noFileMusics) + "');");
            statement.execute("UPDATE music SET img_file_name='' WHERE mid in ('" +String.join("','",noImgMusics) + "');");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        updateCacheSize();
    }

    void updateCacheSize(){
        // 更新和清理缓存
        List<String> noFileMusics=new ArrayList<>(); //需要取消缓存记录的音乐mid列表
        if (cacheSizeSum/1024/1024>Config.getInstance().getInt("cache_size")){
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM music WHERE file_name!='' OR img_file_name!='' ORDER BY refresh_time;");
                while (cacheSizeSum/1024/1024>Config.getInstance().getInt("cache_size")&&rs.next()){
                    Path filePath = Paths.get(cachePath, rs.getString("file_name"));
                    if (filePath.toFile().isFile() && filePath.toFile().exists()){
                        cacheSizeSum-=filePath.toFile().length();
                        if(!filePath.toFile().delete())System.out.println("Delete fail: "+filePath);
                    }
                    filePath = Paths.get(cachePath, rs.getString("img_file_name"));
                    if (filePath.toFile().isFile() && filePath.toFile().exists()){
                        cacheSizeSum-=filePath.toFile().length();
                        if(!filePath.toFile().delete())System.out.println("Delete fail: "+filePath);
                    }
                    noFileMusics.add(rs.getString("mid"));
                }
                statement.execute("UPDATE music SET file_name='',img_file_name='' WHERE mid in ('" +String.join("','",noFileMusics) + "');");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
