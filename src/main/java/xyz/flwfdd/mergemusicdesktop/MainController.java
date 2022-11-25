package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import io.github.palexdev.materialfx.controls.MFXSlider;
import io.github.palexdev.materialfx.controls.MFXTooltip;
import io.github.palexdev.materialfx.enums.SliderEnums;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import io.github.palexdev.materialfx.utils.ToggleButtonsUtil;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoader;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoaderBean;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableDoubleValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextBoundsType;
import org.kordamp.ikonli.javafx.FontIcon;
import xyz.flwfdd.mergemusicdesktop.model.Player;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.net.URL;
import java.util.List;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/18 21:33
 * @implNote 主界面 包含导航菜单、子面板容器、音乐播放控件
 */
public class MainController {
    @FXML
    VBox navBar; //导航栏

    @FXML
    StackPane mainPane; //子面板

    @FXML
    MFXSlider playSlider; //播放进度条
    boolean settingPlaySlider=false;

    @FXML
    MFXSlider volumeSlider; //音量条

    @FXML
    MFXButton muteButton; //静音按钮

    @FXML
    Label nowTimeLabel;

    @FXML
    Label totalTimeLabel;

    @FXML
    MFXButton playButton; //播放暂停

    @FXML
    MFXButton prefButton; //上一首

    @FXML
    MFXButton nextButton; //下一首

    @FXML
    ImageView playImage;

    @FXML
    Label playName;

    @FXML
    Label playArtists;

    @FXML
    Label playAlbum;

    @FXML
    HBox infoPane;

    @FXML
    Pane backgroundPane;

    ToggleGroup toggleGroup;
    Player playerInstance;


    URL loadURL(String path){
        return MainApplication.class.getResource(path);
    }

    ToggleButton createToggle(String icon,String text){
        MFXRectangleToggleNode toggleNode=new MFXRectangleToggleNode(text,new FontIcon(icon+":24"));
        toggleNode.setToggleGroup(toggleGroup);
        return toggleNode;
    }

    void initNavBar(){
        navBar.setSpacing(4);
        toggleGroup=new ToggleGroup();
        ToggleButtonsUtil.addAlwaysOneSelectedSupport(toggleGroup);
        MFXLoader loader=new MFXLoader();
        loader.addView(MFXLoaderBean.of("Search",loadURL("search-view.fxml")).setBeanToNodeMapper(()->createToggle("mdomz-search","搜索")).setDefaultRoot(false).get());
        loader.addView(MFXLoaderBean.of("Playing",loadURL("playing-view.fxml")).setBeanToNodeMapper(()->createToggle("mdral-graphic_eq","播放")).setDefaultRoot(true).get());
        loader.addView(MFXLoaderBean.of("Config",loadURL("config-view.fxml")).setBeanToNodeMapper(()->createToggle("mdrmz-settings","设置")).setDefaultRoot(false).get());
        loader.setOnLoadedAction(beans -> {
            List<ToggleButton> nodes = beans.stream()
                    .map(bean -> {
                        ToggleButton toggle = (ToggleButton) bean.getBeanToNodeMapper().get();
                        toggle.setOnAction(event -> mainPane.getChildren().setAll(bean.getRoot()));
                        if (bean.isDefaultView()) {
                            mainPane.getChildren().setAll(bean.getRoot());
                            toggle.setSelected(true);
                        }
                        return toggle;
                    }).toList();
            navBar.getChildren().setAll(nodes);
        });
        loader.start();
    }

    String formatTime(Double t){
        return String.format("%d:%02d",Math.round(t/60-0.5),Math.round(t)%60);
    }

    void initPlaySliderPopup(){
        //配置弹出时间提示
        playSlider.setPopupSupplier(() -> {
            Label text = new Label();
            text.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            text.setAlignment(Pos.CENTER);
            text.setId("playSliderPopupText");
            text.textProperty().bind(Bindings.createStringBinding(() -> formatTime(playSlider.getValue()), playSlider.valueProperty()));
            text.rotateProperty().bind(Bindings.createDoubleBinding(() -> playSlider.getPopupSide() == SliderEnums.SliderPopupSide.DEFAULT ? 0.0 : 180.0, playSlider.popupSideProperty()));
            VBox.setVgrow(text, Priority.ALWAYS);
            final MFXFontIcon caret = new MFXFontIcon("mfx-caret-down", 22.0);
            caret.setId("playSliderPopupCaret");
            caret.setBoundsType(TextBoundsType.VISUAL);
            caret.setManaged(false);
            VBox container = new VBox(text, caret) {
                protected void layoutChildren() {
                    super.layoutChildren();
                    Orientation orientation = playSlider.getOrientation();
                    double x = orientation == Orientation.HORIZONTAL ? this.getWidth() / 2.0 - caret.prefWidth(-1.0) / 2.0 : this.getHeight();
                    double y = orientation == Orientation.HORIZONTAL ? this.getHeight() : -(caret.prefHeight(-1.0) / 2.0) + this.getHeight() / 2.0;
                    caret.relocate(this.snapPositionX(x), this.snapPositionY(y));
                }
            };
            container.setId("playSliderPopupContent");
            container.setAlignment(Pos.TOP_CENTER);
            container.setMinSize(45.0, 40.0);
            caret.rotateProperty().bind(Bindings.createDoubleBinding(() -> {
                container.requestLayout();
                return playSlider.getOrientation() == Orientation.HORIZONTAL ? 0.0 : -90.0;
            }, playSlider.needsLayoutProperty(), playSlider.rotateProperty(), playSlider.popupSideProperty()));
            container.visibleProperty().addListener(observable -> container.setVisible(settingPlaySlider));
            return container;
        });
    }

    double settingStartTime;
    void initPlayController(){
        // 初始化进度条
        var nowT=playerInstance.nowTimeProperty();
        var totalT=playerInstance.totalTimeProperty();
        nowTimeLabel.textProperty().bind(Bindings.createStringBinding(() -> formatTime(nowT.get()), nowT));
        totalTimeLabel.textProperty().bind(Bindings.createStringBinding(() -> formatTime(totalT.get()), totalT));

        playSlider.maxProperty().bind(totalT);
        playSlider.setEnableKeyboard(false);
        initPlaySliderPopup();
        nowT.addListener(observable -> {
            if(!settingPlaySlider)playSlider.setValue(nowT.get());
        });
        playSlider.setOnMousePressed(mouseEvent -> {
            settingStartTime=Math.round(nowT.get());
            settingPlaySlider=true;
        });
        playSlider.setOnMouseReleased(mouseEvent -> {
            if(settingStartTime!=playSlider.getValue())playerInstance.seek(playSlider.getValue());
            settingPlaySlider=false;
        });

        // 初始化播放按钮
        playButton.setOnAction(e->{
            if(playerInstance.playingProperty().get()){
                playerInstance.pause();
            }else {
                playerInstance.play();
            }
        });

        playerInstance.playingProperty().addListener(observable -> {
            FontIcon icon= (FontIcon) playButton.getGraphic();
            if(playerInstance.playingProperty().get())icon.setIconLiteral("mdrmz-pause");
            else icon.setIconLiteral("mdrmz-play_arrow");
        });
    }

    void initVolumeController(){ //初始化音量控制部分
        volumeSlider.setPopupSupplier(Region::new);
        volumeSlider.valueProperty().bindBidirectional(playerInstance.showVolumeProperty());
        muteButton.setOnAction(e-> playerInstance.setMute(!playerInstance.isMute()));

        playerInstance.realVolumeProperty().addListener(observable -> {
            FontIcon icon= (FontIcon) muteButton.getGraphic();
            double v=((ObservableDoubleValue)observable).get();
            if(v==0){
                icon.setIconLiteral("mdrmz-volume_off");
            } else if (v<=33) {
                icon.setIconLiteral("mdrmz-volume_mute");
            } else if (v<=66) {
                icon.setIconLiteral("mdrmz-volume_down");
            } else icon.setIconLiteral("mdrmz-volume_up");
        });
    }

    void initPlayInfoPane(){ //播放中信息
        var tooltip=new MFXTooltip(infoPane);
        tooltip.setText("MergeMusic!");
        tooltip.install();

        playerInstance.playingMusicProperty().addListener(observable -> {
            Music music=playerInstance.playingMusicProperty().get();

            playName.setText(music.getName());
            playAlbum.setText(music.getAlbumName());
            playArtists.setText(String.join(",",music.getArtists()));
            tooltip.setText(playName.getText()+System.lineSeparator()+
                    "作者："+playArtists.getText()+System.lineSeparator()+
                    "专辑："+playAlbum.getText());

            new Thread(new Task<Void>() {
                @Override
                protected Void call(){
                    Image image=new Image(music.getImg());
                    playImage.setImage(image);
                    if(image.getHeight()>image.getWidth()){
                        playImage.setX(playImage.getFitWidth()*(1-image.getWidth()/image.getHeight())/2);
                        playImage.setY(0);
                    } else {
                        playImage.setX(0);
                        playImage.setY(playImage.getFitHeight()*(1-image.getHeight()/image.getWidth())/2);
                    }
                    var ratio=Math.max(backgroundPane.getWidth()/image.getWidth(),backgroundPane.getHeight()/image.getHeight());
                    backgroundPane.setBackground(new Background(new BackgroundImage(new Image(music.getImg()),BackgroundRepeat.REPEAT,BackgroundRepeat.REPEAT,BackgroundPosition.CENTER,new BackgroundSize(image.getWidth()*ratio,image.getHeight()*ratio,false,false,false,false))));
                    backgroundPane.setEffect(new GaussianBlur(11));
                    return null;
                }
            }).start();
        });

    }

    void initPlayer(){
        playerInstance=Player.getInstance();
    }

    public void initialize() {
        initPlayer();
        initNavBar();
        initPlayController();
        initVolumeController();
        initPlayInfoPane();
    }
}
