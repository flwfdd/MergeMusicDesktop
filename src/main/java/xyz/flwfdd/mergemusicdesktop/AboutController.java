package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.MFXTooltip;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import xyz.flwfdd.mergemusicdesktop.model.Config;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
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
    Text versionText;
    @FXML
    Hyperlink githubLink;
    @FXML
    Hyperlink websiteLink;


    void setLink(Hyperlink link,String url) {
        var tooltip = new MFXTooltip(link);
        tooltip.setText(url);
        tooltip.setShowDelay(Duration.seconds(0.11));
        tooltip.install();
        link.setOnAction(e->MainApplication.instance.getHostServices().showDocument(url));
    }

    void setCircle(Circle c,double rMin,double rMax,double tk){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(()-> c.setRadius(rMin+(rMax-rMin)*(Math.sin(System.currentTimeMillis()/1000.0*tk)+1)/2));
            }
        }, 0, 1000/25);
    }

    void checkVersion(){
        String nowVersion=Config.getInstance().get("version");
        new Thread(()->{
            String text;
            try{
                URL url = new URL("https://desktop.mergemusic.cn/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(11000);
                connection.setReadTimeout(11000);
                connection.setRequestMethod("GET");
                String latestVersion=new Scanner(connection.getInputStream()).useDelimiter("\\A").next();
                if(latestVersion.equals(nowVersion)){
                    text=String.format("当前版本-v%s 已是最新",nowVersion);
                } else {
                    text=String.format("当前版本-v%s 最新版本-v%s",nowVersion,latestVersion);
                }
            } catch (Exception e){
                text=String.format("当前版本-v%s",nowVersion);
                System.out.println("Get version error:"+e);
            }
            String finalText = text;
            Platform.runLater(()->versionText.setText(finalText));
        }).start();

    }

    public void initialize(){
        setLink(githubLink,"https://github.com/flwfdd/MergeMusicDesktop");
        setLink(websiteLink,"https://desktop.mergemusic.cn");
        setCircle(c0,200,300,0.24);
        setCircle(c1,100,200,0.42);
        setCircle(c2,75,175,0.84);
        setCircle(c3,50,150,0.55);
        setCircle(c4,25,100,0.66);
        checkVersion();
    }

}
