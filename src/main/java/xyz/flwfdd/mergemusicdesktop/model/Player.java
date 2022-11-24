package xyz.flwfdd.mergemusicdesktop.model;

import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import xyz.flwfdd.mergemusicdesktop.music.Music;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/23 10:29
 */
public class Player {
    private static Player instance;

    BooleanProperty mute;
    SimpleDoubleProperty showVolume;
    NumberBinding realVolume;

    Media media;
    MediaPlayer player;

    SimpleDoubleProperty totalTime;
    SimpleDoubleProperty nowTime;
    SimpleBooleanProperty playing;

    public SimpleStringProperty imageUrlProperty() {
        return imageUrl;
    }

    SimpleStringProperty imageUrl;

    public SimpleBooleanProperty playingProperty() {
        return playing;
    }


    public SimpleDoubleProperty totalTimeProperty() {
        return totalTime;
    }


    public SimpleDoubleProperty nowTimeProperty() {
        return nowTime;
    }

    public boolean isMute() {
        return mute.get();
    }


    public SimpleDoubleProperty showVolumeProperty() {
        return showVolume;
    }

    public NumberBinding realVolumeProperty() {
        return realVolume;
    }

    public void setMute(boolean mute) {
        this.mute.set(mute);
    }

    public void seek(double t){
        if(player!=null)player.seek(Duration.seconds(t));
    }

    Player(){
        mute=new SimpleBooleanProperty(false);
        showVolume=new SimpleDoubleProperty(24);
        realVolume=showVolume.multiply(new When(mute).then(0).otherwise(1));

        totalTime=new SimpleDoubleProperty(320);
        nowTime=new SimpleDoubleProperty(0);
        playing=new SimpleBooleanProperty(false);
        imageUrl=new SimpleStringProperty("");
    }

    public static Player getInstance(){
        if(instance==null)instance=new Player();
        return instance;
    }

    public void pause(){
        if(player!=null)player.pause();
    }

    public void play(){
        if(player!=null)player.play();
    }


    boolean loading=false;
    public void play(Music music){
        System.out.println("Play:"+music);

        if(player!=null){
            if(player.getMedia().getSource().equals(music.getSrc()))return;
            player.stop();
        }
        if(loading)return;
        loading=true;
        new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                music.load();
                return null;
            }

            @Override
            protected void succeeded() {
                try{
                    imageUrl.set(music.getImg());
                    media=new Media(music.getSrc());
                    player=new MediaPlayer(media);

                    player.muteProperty().bind(mute);
                    player.volumeProperty().bind(realVolume.divide(100));

                    player.play();
                    player.setOnReady(()->{
                        totalTime.set(player.getStopTime().toSeconds());
                        player.currentTimeProperty().addListener((observableValue, t0, t1) -> nowTime.set(t1.toSeconds()));
                    });
                    playing.bind(player.statusProperty().isEqualTo(MediaPlayer.Status.PLAYING));
                    player.setOnEndOfMedia(()-> player.stop());
                }
                catch (Exception e){
                    System.out.println("Media Error"+e);
                }
                loading=false;
            }
        }).start();
    }

    public void add(Music music){
        System.out.println("Add:"+music);
    }
}
