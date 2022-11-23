package xyz.flwfdd.mergemusicdesktop.model;

import xyz.flwfdd.mergemusicdesktop.music.Music;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/23 10:29
 */
public class PlayList {
    private static PlayList instance;

    public static PlayList getInstance(){
        if(instance==null)instance=new PlayList();
        return instance;
    }

    public void play(Music music){
        System.out.println("Play:"+music);
    }

    public void add(Music music){
        System.out.println("Add:"+music);
    }
}
