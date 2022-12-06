package xyz.flwfdd.mergemusicdesktop.music;

import com.alibaba.fastjson2.JSON;
import javafx.beans.InvalidationListener;
import xyz.flwfdd.mergemusicdesktop.model.Config;
import xyz.flwfdd.mergemusicdesktop.model.table.PlayTable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
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

    synchronized Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbURL);
    }

    void initDB(){
        String dbPath= Paths.get(Config.getInstance().getRootPath(), "music.sqlite").toString();
        cachePath=Paths.get(Config.getInstance().getRootPath(),"cache").toString();
        File file=new File(dbPath);
        File cacheFile=new File(cachePath);
        try {
            if(!file.exists()&&!file.createNewFile())System.out.println("create database file error");
            if(!cacheFile.exists()&&!cacheFile.mkdirs())System.out.println("create cache dir error");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dbURL="jdbc:sqlite:"+dbPath;

        try(Connection connection= getConnection();
            Statement statement=connection.createStatement()) {
            String sql;
            //音乐
            sql= """
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
            sql="""
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

    void initList(){
        // 初始化播放列表
        var playTable=PlayTable.getInstance();
        try(Connection connection= getConnection();
            Statement statement=connection.createStatement()) {
            ResultSet rs=statement.executeQuery("SELECT * FROM list WHERE id=1;");
            if(rs.next()){
                List<String> l=string2List(rs.getString("musics"));
                playTable.getMusicList().setAll(selectMusics(l));
            }
            else statement.execute("INSERT INTO list (name,musics) VALUES ('play_list','[]')");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        playTable.getMusicList().addListener((InvalidationListener) observable -> {
            new Thread(()->{
                String sql="UPDATE list SET musics=? WHERE id=1;";
                try(Connection connection= getConnection();
                    PreparedStatement statement=connection.prepareStatement(sql)) {
                    statement.setString(1,list2String(playTable.getMusicList().stream().map(Music::getMid).toList()));
                    statement.execute();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        });
    }

    DB(){
        initDB();
        initList();
    }

    public static DB getInstance(){
        if(instance==null)instance=new DB();
        return instance;
    }

    String list2String(List<String> l){
        return JSON.toJSONString(l);
    }

    List<String> string2List(String s){
        return JSON.parseArray(s,String.class);
    }

    List<Music> selectMusics(List<String> midList){
        // 从数据库中抽取出歌曲列表 不包含源
        Map<String,Music>map=new HashMap<>();
        String sql="SELECT * FROM music WHERE mid in ("+
                midList.stream().map(x->"?").collect(Collectors.joining(",")) +");";
        try(Connection connection= getConnection();
            PreparedStatement statement=connection.prepareStatement(sql)) {
            for(int i=0;i<midList.size();i++)statement.setString(i+1,midList.get(i));
            ResultSet rs=statement.executeQuery();
            while(rs.next()){
                map.put(rs.getString("mid"),Music.getMusic(
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

    public void updateMusic(Music music){
        // 更新数据库
        if(music.getType()!= Music.Type.MUSIC)return;
        String sql="""
            INSERT INTO music (mid,name,lrc,translate_lrc,album_name,artists,refresh_time)
            VALUES (?,?,?,?,?,?,?)
            ON CONFLICT DO UPDATE SET mid=?,name=?,lrc=?,translate_lrc=?,album_name=?,artists=?,refresh_time=?;
            """;
        try(Connection connection= getConnection();
            PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setString(1,music.getMid());
            statement.setString(2,music.getName());
            statement.setString(3,music.getLrc());
            statement.setString(4,music.getTranslateLrc());
            statement.setString(5,music.getAlbumName());
            statement.setString(6,list2String(music.getArtists()));
            statement.setInt(7, (int) (System.currentTimeMillis()/1000));
            statement.setString(8,music.getMid());
            statement.setString(9,music.getName());
            statement.setString(10,music.getLrc());
            statement.setString(11,music.getTranslateLrc());
            statement.setString(12,music.getAlbumName());
            statement.setString(13,list2String(music.getArtists()));
            statement.setInt(14, (int) (System.currentTimeMillis()/1000));
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Pattern illegalFileNamePattern=Pattern.compile("[\\\\/:*?\"<>|]"); //操作系统非法字符
    String getFileName(Music music){
        //生成音乐文件名
        String s=music.getName()+" - "+String.join(",",music.getArtists())+" - "+music.getMid();
        Matcher matcher=illegalFileNamePattern.matcher(s);
        return matcher.replaceAll("");
    }
    String getMusicFileName(Music music){
        return getFileName(music)+music.getPlatform().getExtension();
    }

    String getImgFileName(Music music){
        return getFileName(music)+".jpg";
    }

    final static int buffSize=102400; //下载片大小
    boolean download(String src, String path, Map<String,String> headers){
        File file=Paths.get(path).toFile();
        try{
            URL url=new URL(src);
            URLConnection con=url.openConnection();
            headers.forEach(con::setRequestProperty);
            InputStream is=con.getInputStream();
            byte[]bs=new byte[buffSize];
            FileOutputStream os=new FileOutputStream(file);
            int len;
            while((len=is.read(bs))!=-1){
                os.write(bs,0,len);
            }
            os.close();
            is.close();
        } catch (Exception e) {
            System.out.println("Download error:"+e);
            file.deleteOnExit();
            return false;
        }
        return true;
    }

    void cacheMusic(Music music){
        // 缓存音乐
        if(music.getSrc()!=null&&!music.getSrc().isBlank()&&!music.getSrc().startsWith("file")){
            new Thread(()->{
                if(download(music.getSrc(),Paths.get(cachePath,getMusicFileName(music)).toString(),music.getHeaders())){
                    String sql="UPDATE music SET file_name=? WHERE mid=?;";
                    try(Connection connection= getConnection();
                        PreparedStatement statement=connection.prepareStatement(sql)) {
                        statement.setString(1,getMusicFileName(music));
                        statement.setString(2,music.getMid());
                        statement.execute();
                    } catch (SQLException e) {
                        System.out.println("Cache src error:"+music);
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
        if(music.getImg()!=null&&!music.getImg().isBlank()&&!music.getImg().startsWith("file")){
            new Thread(()->{
                if(download(music.getImg(),Paths.get(cachePath,getImgFileName(music)).toString(),music.getHeaders())){
                    String sql="UPDATE music SET img_file_name=? WHERE mid=?;";
                    try(Connection connection= getConnection();
                        PreparedStatement statement=connection.prepareStatement(sql)) {
                        statement.setString(1,getImgFileName(music));
                        statement.setString(2,music.getMid());
                        statement.execute();
                    } catch (SQLException e) {
                        System.out.println("Cache image error:"+music);
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    Music getCacheMusic(String mid){
        Music music = null;
        // 获取缓存的歌曲
        String sql="SELECT * FROM music WHERE mid=?;";
        try(Connection connection= getConnection();
            PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setString(1,mid);
            ResultSet rs=statement.executeQuery();
            if(rs.next()){
                String src="",img="";
                Path filePath=Paths.get(cachePath,rs.getString("file_name"));
                if(filePath.toFile().isFile()&&filePath.toFile().exists())src=filePath.toUri().toString();
                filePath=Paths.get(cachePath,rs.getString("img_file_name"));
                if(filePath.toFile().isFile()&&filePath.toFile().exists())img=filePath.toUri().toString();
                music=Music.getMusic(
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
        if(music!=null)updateMusic(music);
        return music;
    }

}
