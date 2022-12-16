package xyz.flwfdd.mergemusicdesktop.model.table;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import xyz.flwfdd.mergemusicdesktop.model.Config;
import xyz.flwfdd.mergemusicdesktop.music.DB;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/15 12:54
 * @implNote 收藏夹面板的数据模型
 */

public class FavoriteTable extends MusicTable {

    static DB db = DB.getInstance();
    static FavoriteTable instance;
    Map<Integer, String> lists;
    SimpleIntegerProperty nowID = new SimpleIntegerProperty();
    public SimpleStringProperty title = new SimpleStringProperty();

    public int newList(String name) {
        int id = db.createList(name);
        lists.put(id, name);
        return id;
    }

    public void favoriteMusic(int id, Music music) {
        ObservableList<Music> l;
        if (id == nowID.get()) l = musicList;
        else l = FXCollections.observableArrayList(db.getList(id));
        AtomicBoolean flag = new AtomicBoolean(false);
        l.forEach(x -> {
            if (x.getMid().equals(music.getMid())) flag.set(true);
        });
        if (flag.get()) Config.getInstance().setMsg("已经存在啦awa");
        else {
            l.add(music);
            db.setList(id, l);
            Config.getInstance().setMsg("收藏 " + music.getName() + " 到 " + lists.get(id) + " 成功OvO");
        }
    }

    public void favoriteMusics(int id, List<Music> musics) {
        ObservableList<Music> l;
        if (id == nowID.get()) l = musicList;
        else l = FXCollections.observableArrayList(db.getList(id));
        Set<String> set=new HashSet<>();
        l.forEach(x -> set.add(x.getMid()));
        AtomicInteger ct= new AtomicInteger();
        musics.forEach(music -> {
            if(!set.contains(music.getMid())){
                ct.getAndIncrement();
                l.add(music);
            }
            set.add(music.getMid());
        });
        db.setList(id, l);
        Config.getInstance().setMsg("收藏"+ct.get()+"首音乐到 " + lists.get(id) + " 成功OvO");
    }

    public void deleteMusic(Music music){
        for (int i = musicList.size() - 1; i >= 0; i--) {
            if (musicList.get(i).getMid().equals(music.getMid())) {
                musicList.remove(i);
                db.setList(nowID.get(),musicList);
                return;
            }
        }
    }

    public void changeList(int id) {
        nowID.set(id);
    }

    public void deleteList(int id) {
        db.deleteList(id);
        lists.remove(id);
        if(nowID.get()==id){
            nowID.set(0);
            if (!lists.isEmpty()) nowID.set(lists.keySet().stream().toList().get(0));
        }
    }

    public void playAll(){
        if(musicList.isEmpty())return;
        PlayTable.getInstance().clear();
        PlayTable.getInstance().play(musicList.get(0));
        addAll();
    }

    public void addAll(){
        musicList.forEach(music -> PlayTable.getInstance().add(music,false));
        Config.getInstance().setMsg("已全部添加到播放列表");
    }

    public String getNowName(){
        return lists.get(nowID.get());
    }

    public void rename(String s){
        if(nowID.get()==0)return;
        db.renameList(nowID.get(),s);
        lists.put(nowID.get(),s);
        title.set(getNowName() + "（共" + musicList.size() + "首）");
    }

    @Override
    List<Operation> getMenus(Music music) {
        //右键菜单
        List<Operation> menus = new ArrayList<>();
        menus.add(new MusicTable.Operation("mdral-delete", "删除", () -> Platform.runLater(()-> deleteMusic(music))));
        menus.addAll(super.getMenus(music));
        return menus;
    }

    FavoriteTable() {
        super();

        lists = new TreeMap<>();
        lists = db.getLists();

        nowID.addListener(observable -> {
            if (nowID.get() == 0) {
                musicList.clear();
                title.set("请选择收藏夹awa");
            } else {
                musicList.setAll(db.getList(nowID.get()));
                title.set(getNowName() + "（共" + musicList.size() + "首）");
            }
        });

        if (lists.isEmpty()) nowID.set(0);
        else nowID.set(lists.keySet().stream().toList().get(0));
    }

    public static FavoriteTable getInstance() {
        if (instance == null) instance = new FavoriteTable();
        return instance;
    }
}
