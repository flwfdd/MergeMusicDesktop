package xyz.flwfdd.mergemusicdesktop.model;

import javafx.beans.property.SimpleStringProperty;

import java.io.*;
import java.net.URLDecoder;
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
    SimpleStringProperty msgProperty;

    public enum Type {
        TEXT, INT, DOUBLE
    }

    public static class ConfigItem {
        public Type type;
        public String key, description;
        public boolean editable;
        public SimpleStringProperty valueProperty;

        ConfigItem(String key, String value, String description, boolean editable, Type type) {
            this.key = key;
            this.description = description;
            this.editable = editable;
            this.type = type;
            valueProperty = new SimpleStringProperty(value);
        }
    }

    public List<ConfigItem> configItems;

    void initConfigItem(String key, String value, String description, boolean editable, Type type) {
        if (!has(key)) set(key, value);
        else value = get(key);
        configItems.add(new ConfigItem(key, value, description, editable, type));
    }

    public void initConfigItems() { //初始化项目
        if (new File(configPath).exists()) load();
        configItems = new ArrayList<>();
        initConfigItem("cloud_music_api_url", "https://service-r6uorko5-1255944436.bj.apigw.tencentcs.com/release", "网易云音乐API", true, Type.TEXT);
        initConfigItem("cloud_music_cookie", "", "网易云音乐Cookie", true, Type.TEXT);
        initConfigItem("spectrum_num_bands", "16", "音频可视化通道数", false, Type.INT);
        initConfigItem("spectrum_threshold", "100", "音频可视化阈值(>0,dB)", false, Type.INT);
        initConfigItem("spectrum_interval", "0.04", "音频可视化时间窗口(s)", false, Type.DOUBLE);
        initConfigItem("spectrum_delay", "0.42", "音频可视化延迟时间(s)", false, Type.DOUBLE);
        initConfigItem("spectrum_smooth_ratio", "0.24", "平滑比例(0~1)", false, Type.DOUBLE);
    }

    public void resetConfigItems() {
        configItems.forEach(item -> item.valueProperty.set(get(item.key)));
    }

    public void saveConfigItems() {
        configItems.forEach(item -> set(item.key, item.valueProperty.get()));
    }

    Config() { //初始化配置文件
        msgProperty = new SimpleStringProperty();
        properties = new Properties();
        configPath = Paths.get(getRootPath(), "config.properties").toString();
        initConfigItems();
    }

    void load() {
        try (var reader = new InputStreamReader(new FileInputStream(configPath), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void store() {
        try (var writer = new OutputStreamWriter(new FileOutputStream(configPath), StandardCharsets.UTF_8)) {
            properties.store(writer, "");
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

    public int getInt(String key) {
        return (int) Math.round(getDouble(key));
    }

    public double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
        store();
    }

    public boolean has(String key) {
        return properties.containsKey(key);
    }

    public SimpleStringProperty getMsgProperty() {
        return msgProperty;
    }

    public void setMsg(String msg) {
        msgProperty.set(msg);
    }

    public String getRootPath() {
        if (rootPath == null) {
            String path = Config.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
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
