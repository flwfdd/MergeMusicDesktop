module xyz.flwfdd.mergemusicdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires MaterialFX;
    requires VirtualizedFX;
    requires org.kordamp.ikonli.javafx;
    requires com.alibaba.fastjson2;
    requires java.sql;


    opens xyz.flwfdd.mergemusicdesktop to javafx.fxml;
    opens xyz.flwfdd.mergemusicdesktop.dialog to javafx.fxml;
    exports xyz.flwfdd.mergemusicdesktop;
}