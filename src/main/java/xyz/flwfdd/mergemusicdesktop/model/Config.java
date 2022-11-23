package xyz.flwfdd.mergemusicdesktop.model;

import javafx.beans.property.SimpleStringProperty;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/23 19:36
 * @implNote 全局配置 使用单例
 */
public class Config {
    static Config instance;
    String rootPath;
    String configPath;
    Properties properties;

    public enum Type{
        TEXT
    }

    public static class ConfigItem{
        public Type type;
        public String key,description;
        public boolean editable;
        public SimpleStringProperty valueProperty;

        ConfigItem(String key,String value,String description,boolean editable,Type type){
            this.key=key;
            this.description=description;
            this.editable=editable;
            this.type=type;
            valueProperty=new SimpleStringProperty(value);
        }
    }

    public List<ConfigItem>configItems;

    void initConfigItem(String key,String value,String description,boolean editable,Type type){
        if(!has(key))set(key,value);
        else value=get(key);
        configItems.add(new ConfigItem(key, value,description,editable,type));
    }
    public void initConfigItems(){ //初始化项目
        if (new File(configPath).exists()) load();
        configItems=new ArrayList<>();
        initConfigItem("cloud_music_api_url", "http://148.70.105.159:3000","网易云音乐API",true,Type.TEXT);
        initConfigItem("cloud_music_cookie", "","网易云音乐Cookie",true,Type.TEXT);
    }

    public void resetConfigItems(){
        configItems.forEach(item-> item.valueProperty.set(get(item.key)));
    }

    public void saveConfigItems(){
        configItems.forEach(item-> set(item.key,item.valueProperty.get()));
    }

    Config() { //初始化配置文件
        properties = new Properties();
        configPath = Paths.get(getRootPath(), "config.properties").toString();
        initConfigItems();
    }

    void load(){
        try (var reader = new InputStreamReader(new FileInputStream(configPath), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void store(){
        try (var writer = new OutputStreamWriter(new FileOutputStream(configPath), StandardCharsets.UTF_8)) {
            properties.store(writer,"");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Config getInstance() {
        if (instance == null) instance = new Config();
        return instance;
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public void set(String key,String value) {
        properties.setProperty(key,value);
        store();
    }

    public boolean has(String key){
        return properties.containsKey(key);
    }

    public String getRootPath() {
        if (rootPath == null) {
            String path = Config.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            if (System.getProperty("os.name").contains("dows")) {
                path = path.substring(1);
            }
            if (path.endsWith(".jar")) {
                path = path.substring(0, path.lastIndexOf("."));
                rootPath = path.substring(0, path.lastIndexOf("/"));
            } else rootPath = path.replace("target/classes/", "");
        }
        return rootPath;
    }
}
