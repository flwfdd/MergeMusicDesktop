package xyz.flwfdd.mergemusicdesktop.model.table;

import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import xyz.flwfdd.mergemusicdesktop.model.Player;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/30 21:33
 * @implNote 播放列表
 */
public class PlayTable extends MusicTable{

    public enum LoopType{
        LIST,SINGLE,RANDOM;

        public String getIcon(){
            return switch (this){
                case LIST -> "mdral-east";
                case SINGLE -> "mdrmz-replay";
                case RANDOM -> "mdrmz-shuffle";
            };
        }

        public LoopType getNext(){
            return switch (this){
                case LIST -> SINGLE;
                case SINGLE -> RANDOM;
                case RANDOM -> LIST;
            };
        }
    }
    static PlayTable instance;

    SimpleObjectProperty<LoopType> loopType =new SimpleObjectProperty<>(LoopType.LIST);

    volatile boolean loading=false;

    public SimpleObjectProperty<LoopType> loopTypeProperty() {
        return loopType;
    }

    final int history_max=24;
    Deque<Music> history=new LinkedList<>();

    public void playPref(){
        switch (loopType.get()){
            case LIST -> {
                if(musicList.isEmpty())return;
                var playingMid=Player.getInstance().playingMusicProperty().get().getMid();
                for(int i=0;i<musicList.size();i++){
                    if(musicList.get(i).getMid().equals(playingMid)){
                        play(musicList.get((i+musicList.size()-1)%musicList.size()));
                        return;
                    }
                }
                play(musicList.get(0));
            }
            case SINGLE -> Player.getInstance().play();
            case RANDOM -> {
                if(musicList.isEmpty())return;
                if(history.isEmpty()) play(musicList.get((int)(Math.random()*musicList.size())));
                else {
                    history.removeLast();
                    if(!history.isEmpty()){
                        var m=history.getLast();
                        history.removeLast();
                        play(m);
                    }
                    else play(musicList.get((int)(Math.random()*musicList.size())));
                }
            }
        }
    }

    public void playNext(){
        switch (loopType.get()){
            case LIST -> {
                if(musicList.isEmpty())return;
                var playingMid=Player.getInstance().playingMusicProperty().get().getMid();
                for(int i=0;i<musicList.size();i++){
                    if(musicList.get(i).getMid().equals(playingMid)){
                        play(musicList.get((i+1)%musicList.size()));
                        return;
                    }
                }
                play(musicList.get(0));
            }
            case SINGLE -> Player.getInstance().play();
            case RANDOM -> {
                if(musicList.isEmpty())return;
                play(musicList.get((int)(Math.random()*musicList.size())));
            }
        }
    }

    public void play(Music music){
        System.out.println("Play:"+music);
        history.addLast(music);
        if(history.size()>history_max)history.removeFirst();

        if(music.getType()== Music.Type.MUSIC){
            add(music);
            Player.getInstance().play(music);
        }
        else{
            if(loading)return;
            loading=true;
            new Thread(new Task<Void>() {
                List<Music> l;
                @Override
                protected Void call(){
                    l=music.unfold();
                    return null;
                }

                @Override
                protected void succeeded() {
                    if(l!=null){
                        musicList.setAll(l);
                        if(!musicList.isEmpty())Player.getInstance().play(musicList.get(0));
                    }
                    loading=false;
                }
            }).start();
        }
    }

    public void add(Music music){
        System.out.println("Add:"+music);
        if(music.getType()== Music.Type.MUSIC){
            for (Music value : musicList) {
                if (value.getMid().equals(music.getMid())) return;
            }
            musicList.add(music);
        }
        else{
            if(loading)return;
            loading=true;
            new Thread(new Task<Void>() {
                List<Music> l;
                @Override
                protected Void call(){
                    l=music.unfold();
                    return null;
                }

                @Override
                protected void succeeded() {
                    if(l!=null)musicList.addAll(l);
                    loading=false;
                }
            }).start();
        }
    }

    void delete(Music music){
        for(int i=musicList.size()-1;i>=0;i--){
            if(musicList.get(i).getMid().equals(music.getMid())){
                musicList.remove(i);
                return;
            }
        }
    }

    @Override
    List<Operation> getOperations(Music music){
        List<Operation> operations= new ArrayList<>();
        operations.add(new MusicTable.Operation("mdrmz-play_arrow", "播放",()-> play(music)));
        operations.add(new MusicTable.Operation("mdral-delete", "删除",()-> delete(music)));
        return operations;
    }

    public static PlayTable getInstance(){
        if(instance==null)instance=new PlayTable();
        return instance;
    }
}
