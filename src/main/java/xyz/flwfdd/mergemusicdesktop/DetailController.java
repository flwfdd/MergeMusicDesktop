package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;
import xyz.flwfdd.mergemusicdesktop.model.Config;
import xyz.flwfdd.mergemusicdesktop.music.Music;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/14 14:04
 * @implNote
 */

public class DetailController {

    @FXML
    HBox headBar;
    @FXML
    MFXTextField midField;
    @FXML
    MFXTextField nameField;
    @FXML
    MFXTextField artistsField;
    @FXML
    MFXTextField albumField;
    @FXML
    MFXTextField srcField;
    @FXML
    MFXTextField imgField;
    @FXML
    MFXTextField lrcField;
    @FXML
    MFXTextField translateLrcField;


    @FXML
    void onClose(){
        DetailController.stage.close();
    }

    FontIcon createCopyButton(StringProperty text){
        FontIcon icon=new FontIcon("mdral-content_copy:17");
        icon.hoverProperty().addListener(observable -> icon.fillProperty().set(Paint.valueOf(icon.isHover()?"#00ABD6":"#004354")));
        icon.setOnMouseClicked(e->{
            var content=new ClipboardContent();
            content.put(DataFormat.PLAIN_TEXT,text.get());
            Clipboard.getSystemClipboard().setContent(content);
            Config.getInstance().setMsg("复制成功OvO");
        });
        return icon;
    }

    void setMusic(Music music){
        music.load();
        mid.set(music.getMid());
        name.set(music.getName());
        artists.set(String.join(",",music.getArtists()));
        album.set(music.getAlbumName());
        src.set(music.getSrc());
        img.set(music.getImg());
        lrc.set(music.getLrc());
        translateLrc.set(music.getTranslateLrc());

        midField.setText(music.getMid());
        nameField.setText(music.getName());
        artistsField.setText(String.join(",",music.getArtists()));
        albumField.setText(music.getAlbumName());
        srcField.setText(music.getSrc());
        imgField.setText(music.getImg());
        lrcField.setText(music.getLrc());
        translateLrcField.setText(music.getTranslateLrc());
    }

    static Stage stage;
    static DetailController controller;


    double xOffset=0;
    double yOffset=0;
    SimpleStringProperty mid,name,artists,album,src,img,lrc,translateLrc;
    public void initialize() {
        mid=new SimpleStringProperty();
        name=new SimpleStringProperty();
        artists=new SimpleStringProperty();
        album=new SimpleStringProperty();
        src=new SimpleStringProperty();
        img=new SimpleStringProperty();
        lrc=new SimpleStringProperty();
        translateLrc=new SimpleStringProperty();
        midField.setTrailingIcon(createCopyButton(mid));
        nameField.setTrailingIcon(createCopyButton(name));
        artistsField.setTrailingIcon(createCopyButton(artists));
        albumField.setTrailingIcon(createCopyButton(album));
        srcField.setTrailingIcon(createCopyButton(src));
        imgField.setTrailingIcon(createCopyButton(img));
        lrcField.setTrailingIcon(createCopyButton(lrc));
        translateLrcField.setTrailingIcon(createCopyButton(translateLrc));

        //窗口拖动
        headBar.setOnMousePressed(event -> {
            xOffset=event.getSceneX();
            yOffset=event.getSceneY();
        });

        headBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    public static void showDetail(Music music){
        if(controller==null)try{
            FXMLLoader loader=new FXMLLoader(DetailController.class.getResource("detail-view.fxml"));
            stage=new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.getScene().setFill(Color.TRANSPARENT);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            controller=loader.getController();
        } catch (Exception e){
            System.out.println("open detail error:"+e);
        }

        controller.setMusic(music);
        stage.show();
    }
}
