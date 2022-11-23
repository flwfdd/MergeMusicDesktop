package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import io.github.palexdev.materialfx.controls.MFXSlider;
import io.github.palexdev.materialfx.enums.SliderEnums;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import io.github.palexdev.materialfx.utils.ToggleButtonsUtil;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoader;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoaderBean;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextBoundsType;
import org.kordamp.ikonli.javafx.FontIcon;

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

    ToggleGroup toggleGroup;

    URL loadURL(String path){
        return MainApplication.class.getResource(path);
    }

    ToggleButton createToggle(String icon,String text){
        MFXRectangleToggleNode toggleNode=new MFXRectangleToggleNode(text,new FontIcon(icon+":24"));
        toggleNode.setToggleGroup(toggleGroup);
        return toggleNode;
    }

    void initNevBar(){
        toggleGroup=new ToggleGroup();
        ToggleButtonsUtil.addAlwaysOneSelectedSupport(toggleGroup);
        MFXLoader loader=new MFXLoader();
        loader.addView(MFXLoaderBean.of("Search",loadURL("search-view.fxml")).setBeanToNodeMapper(()->createToggle("mdomz-search","搜索")).setDefaultRoot(true).get());
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

    void initPlaySlider(){ //初始化进度条
        //配置弹出时间提示
        playSlider.setPopupSupplier(() -> {
            Label text = new Label();
            text.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            text.setAlignment(Pos.CENTER);
            text.setId("playSliderPopupText");
            text.textProperty().bind(Bindings.createStringBinding(() -> String.format("%d:%02d",Math.round(playSlider.getValue()/60-0.5),Math.round(playSlider.getValue())%60), playSlider.valueProperty()));
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
            return container;
        });
    }

    public void initialize() {
        initNevBar();
        initPlaySlider();
    }
}
