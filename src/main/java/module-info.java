module xyz.flwfdd.mergemusicdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires MaterialFX;
    requires VirtualizedFX;
    requires okhttp3;
    requires com.alibaba.fastjson2;


    opens xyz.flwfdd.mergemusicdesktop to javafx.fxml;
    exports xyz.flwfdd.mergemusicdesktop;
}