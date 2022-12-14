package xyz.flwfdd.mergemusicdesktop;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import xyz.flwfdd.mergemusicdesktop.model.Config;
import xyz.flwfdd.mergemusicdesktop.model.Player;

import java.util.*;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/24 17:05
 */
public class PlayingController {
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
    Text lrcText;

    @FXML
    Text translateLrcText;

    int threshold, offset;
    double ratio;
    List<Double> history_a = new ArrayList<>();
    List<Double> history_b = new ArrayList<>();
    List<Double> history_c = new ArrayList<>();
    List<Double> history_d = new ArrayList<>();

    SortedMap<Double, String> lrcMap, translateLrcMap;

    Player player;

    Config config;

    void updateCircle(double a, double b, double c, double d) { //更新音频可视化
        offset = (int) Math.round(config.getDouble("spectrum_delay") / player.getInterval());
        ratio = config.getDouble("spectrum_smooth_ratio");
        threshold = -player.getThreshold();

        history_a.add((a + threshold) / threshold);
        history_b.add((b + threshold) / threshold);
        history_c.add((c + threshold) / threshold);
        history_d.add((d + threshold) / threshold);
        while (history_a.size() > offset) {
            a = history_a.get(0);
            history_a.remove(0);
        }
        while (history_b.size() > offset) {
            b = history_b.get(0);
            history_b.remove(0);
        }
        while (history_c.size() > offset) {
            c = history_c.get(0);
            history_c.remove(0);
        }
        while (history_d.size() > offset) {
            d = history_d.get(0);
            history_d.remove(0);
        }
        var e = (a + b + c + d) / 4;
        e = e * 100 + 200;
        a = a * 100 + 100;
        b = b * 100 + 75;
        c = c * 100 + 50;
        d = d * 100 + 25;
        e = c0.getRadius() * 0.84 + e * (1 - 0.84);
        a = c1.getRadius() * ratio + a * (1 - ratio);
        b = c2.getRadius() * ratio + b * (1 - ratio);
        c = c3.getRadius() * ratio + c * (1 - ratio);
        d = c4.getRadius() * ratio + d * (1 - ratio);
        c0.setRadius(e);
        c1.setRadius(a);
        c2.setRadius(b);
        c3.setRadius(c);
        c4.setRadius(d);
    }

    SortedMap<Double, String> decodeLyrics(String lrcString) { //解析歌词
        SortedMap<Double, String> lrcMap = new TreeMap<>(Comparator.reverseOrder());
        if (lrcString == null) return lrcMap;
        Arrays.asList(lrcString.split("\n")).forEach(s -> {
            try {
                s = s.substring(1);
                var x1 = s.indexOf(":");
                var x2 = s.indexOf("]");
                if (x1 != -1 && x2 != -1) {
                    double t = Double.parseDouble(s.substring(0, x1)) * 60 + Double.parseDouble(s.substring(x1 + 1, x2));
                    lrcMap.put(t, s.substring(x2 + 1));
                }
            } catch (Exception ignored) {
            }
        });
        return lrcMap;
    }

    public void initialize() {
        config = Config.getInstance();
        player = Player.getInstance();
        // 初始化音频可视化

        player.spectrumProperty().addListener((InvalidationListener) observable -> {
            var l = Player.getInstance().spectrumProperty();
            int sz = l.size();
            updateCircle(l.get(sz / 16), l.get(sz / 8), l.get(sz / 4), l.get(sz / 2));
        });

        //初始化歌词
        player.playingMusicProperty().addListener(observable -> {
            lrcMap = decodeLyrics(player.playingMusicProperty().get().getLrc());
            translateLrcMap = decodeLyrics(player.playingMusicProperty().get().getTranslateLrc());
            lrcText.setText("");
            translateLrcText.setText("");
        });

        player.nowTimeProperty().addListener(observable -> {
            if (config.getInt("show_lrc") == 1) {
                var t = player.nowTimeProperty().get();
                for (Double i : lrcMap.keySet()) {
                    if (t >= i) {
                        lrcText.setText(lrcMap.get(i));
                        break;
                    }
                }
                for (var i : translateLrcMap.keySet()) {
                    if (t >= i) {
                        translateLrcText.setText(translateLrcMap.get(i));
                        break;
                    }
                }
            } else {
                lrcText.setText("");
                translateLrcText.setText("");
            }
        });
    }
}
