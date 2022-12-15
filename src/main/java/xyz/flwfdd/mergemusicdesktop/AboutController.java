package xyz.flwfdd.mergemusicdesktop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.shape.Circle;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/15 23:13
 * @implNote 关于页面
 */

public class AboutController {
    @FXML
    Circle c0;
    @FXML
    Circle c1;
    @FXML
    Circle c2;
    @FXML
    Circle c3;
    @FXML
    Circle c4;
    @FXML
    void onGitHub(){
        try {
            Desktop.getDesktop().browse(URI.create("https://github.com/flwfdd/MergeMusicDesktop"));
        } catch (IOException e) {
            System.out.println("open link error:"+e);
        }
    }
    @FXML
    void onWebsite(){
        try {
            Desktop.getDesktop().browse(URI.create("http://desktop.mergemusic.cn"));
        } catch (IOException e) {
            System.out.println("open link error:"+e);
        }
    }

    void setCircle(Circle c,double rMin,double rMax,double tk){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(()-> c.setRadius(rMin+(rMax-rMin)*(Math.sin(System.currentTimeMillis()/1000.0*tk)+1)/2));
            }
        }, 0, 1000/25);
    }

    public void initialize(){
        setCircle(c0,200,300,0.24);
        setCircle(c1,100,200,0.42);
        setCircle(c2,75,175,0.84);
        setCircle(c3,50,150,0.55);
        setCircle(c4,25,100,0.66);
    }

}
