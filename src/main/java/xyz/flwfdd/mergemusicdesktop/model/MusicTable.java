package xyz.flwfdd.mergemusicdesktop.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.List;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/17 22:17
 */
public class MusicTable {
    final int limit;
    String searchKey;
    Music.Platform searchPlatform;
    Music.Type searchType;
    int offset;
    boolean loading=false; //保证单个搜索线程

    boolean haveNext=false; //是否可以加载更多

    ObservableList<Music> musicList;

    public ObservableList<Music> getMusicList() {
        return musicList;
    }

    public MusicTable(){
        limit=24;
        musicList= FXCollections.observableArrayList();
    }

    public void search(String keyword, Music.Platform platform, Music.Type type){
        if(loading)return;
        searchKey=keyword;
        searchPlatform=platform;
        searchType=type;
        offset=0;
        musicList.clear();
        haveNext=true;
        searchNext();
    }

    public void searchNext(){ //获取下一页
        if(loading||!haveNext)return;
        loading=true;
        new Thread(new Task<Void>() {
            List<Music>l;
            @Override
            protected Void call(){
                l=Music.search(searchKey,searchPlatform,searchType,limit,offset);
                return null;
            }

            @Override
            protected void succeeded() {
                musicList.addAll(l);
                haveNext=(l.size()!=0);
                offset++;
                loading=false;
            }
        }).start();
    }
}
