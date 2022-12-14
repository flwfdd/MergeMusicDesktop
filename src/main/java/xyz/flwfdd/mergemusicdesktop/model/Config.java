package xyz.flwfdd.mergemusicdesktop.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

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
    Stage mainStage;

    public enum Type {
        TEXT, INT, DOUBLE
    }

    public static class ConfigItem {
        public Type type;
        public String key, description;
        public boolean editable;
        public Function<String, Boolean> checker;
        public SimpleStringProperty valueProperty;

        ConfigItem(String key, String value, String description, boolean editable, Type type, Function<String, Boolean> checker) {
            this.key = key;
            this.description = description;
            this.editable = editable;
            this.type = type;
            this.checker = checker;
            valueProperty = new SimpleStringProperty(value);
        }
    }

    public List<ConfigItem> configItems;

    void initConfigItem(String key, String value, String description, boolean editable, Type type, Function<String, Boolean> checker) {
        if (!has(key)) set(key, value);
        else value = get(key);
        configItems.add(new ConfigItem(key, value, description, editable, type, checker));
    }

    public void initConfigItems() { //初始化项目
        if (new File(configPath).exists()) load();
        configItems = new ArrayList<>();
        initConfigItem("cloud_music_api_url", "https://service-r6uorko5-1255944436.bj.apigw.tencentcs.com/release", "网易云音乐API", true, Type.TEXT, s -> true);
        initConfigItem("cloud_music_cookie", "", "网易云音乐Cookie", true, Type.TEXT, s -> true);
        initConfigItem("qq_music_cookie", "", "QQ音乐Cookie", true, Type.TEXT, s -> true);
        initConfigItem("bili_cookie", "", "B站Cookie", true, Type.TEXT, s -> true);
        initConfigItem("show_lrc", "1", "是否显示歌词(0/1)", true, Type.INT, s -> {
            int x = Integer.parseInt(s);
            return x==0||x==1;
        });
        initConfigItem("cache_size", "2048", "最大缓存空间(MB)", true, Type.INT, s -> {
            int x = Integer.parseInt(s);
            return !(x < 0);
        });
        initConfigItem("spectrum_delay", "0.42", "音频可视化延迟时间(s)", true, Type.DOUBLE, s -> {
            double x = Double.parseDouble(s);
            return x>=0&&x<=11;
        });
        initConfigItem("spectrum_smooth_ratio", "0.24", "音频可视化平滑系数(0~1)", true, Type.DOUBLE, s -> {
            double x=Double.parseDouble(s);
            return x>=0&&x<=1;
        });
        initConfigItem("spectrum_threshold", "100", "音频可视化采样灵敏度(>0,dB)", true, Type.INT, s -> {
            int x=Integer.parseInt(s);
            return x>0;
        });
        initConfigItem("spectrum_num_bands", "16", "音频可视化采样通道数(1-512)", true, Type.INT, s -> {
            int x=Integer.parseInt(s);
            return x>0&&x<=512;
        });
        initConfigItem("spectrum_interval", "0.04", "音频可视化采样时间(s)", true, Type.DOUBLE, s -> {
            double x=Double.parseDouble(s);
            return x>=0.01;
        });
    }

    public void resetConfigItems() {
        configItems.forEach(item -> item.valueProperty.set(get(item.key)));
    }

    public void saveConfigItems() {
        for (ConfigItem configItem : configItems) {
            try {
                if (!configItem.checker.apply(configItem.valueProperty.get())) {
                    setMsg(configItem.description + " 参数无效 请修改awa");
                    return;
                }
            } catch (Exception e) {
                setMsg(configItem.description + " 参数无效 请修改awa");
                return;
            }
        }
        configItems.forEach(item -> set(item.key, item.valueProperty.get()));
        setMsg("保存成功OvO");
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

    public void setMainStage(Stage stage){
        mainStage=stage;
    }

    File lastDir=null;
    public File chooseDir(){
        DirectoryChooser directoryChooser=new DirectoryChooser();
        if(lastDir!=null)directoryChooser.setInitialDirectory(lastDir);
        File file=directoryChooser.showDialog(mainStage);
        if(file!=null)lastDir=file;
        return file;
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
