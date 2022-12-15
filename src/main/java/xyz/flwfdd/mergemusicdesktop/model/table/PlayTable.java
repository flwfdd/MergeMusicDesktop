package xyz.flwfdd.mergemusicdesktop.model.table;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import xyz.flwfdd.mergemusicdesktop.model.Config;
import xyz.flwfdd.mergemusicdesktop.model.Player;
import xyz.flwfdd.mergemusicdesktop.music.DB;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.*;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/30 21:33
 * @implNote 播放列表
 */
public class PlayTable extends MusicTable {

    public enum LoopType {
        LIST, SINGLE, RANDOM;

        public String getIcon() {
            return switch (this) {
                case LIST -> "mdral-east";
                case SINGLE -> "mdrmz-replay";
                case RANDOM -> "mdrmz-shuffle";
            };
        }

        public LoopType getNext() {
            return switch (this) {
                case LIST -> SINGLE;
                case SINGLE -> RANDOM;
                case RANDOM -> LIST;
            };
        }

        public static int toInt(LoopType loopType) {
            return switch (loopType) {
                case LIST -> 1;
                case SINGLE -> 2;
                case RANDOM -> 3;
            };
        }

        public static LoopType valueOf(int x) {
            return switch (x) {
                case 1 -> LIST;
                case 2 -> SINGLE;
                case 3 -> RANDOM;
                default -> null;
            };
        }
    }

    static PlayTable instance;

    SimpleObjectProperty<LoopType> loopType = new SimpleObjectProperty<>();

    volatile boolean loading = false;

    public SimpleObjectProperty<LoopType> loopTypeProperty() {
        return loopType;
    }

    final int historyMax = 24;
    Deque<Music> history = new LinkedList<>();
    Deque<List<Music>> musicListHistory = new LinkedList<>();
    final int musicListHistoryMax = 24;

    PlayTable() {
        super();

        loopType.set(LoopType.valueOf(Config.getInstance().getInt("loop_type")));
        loopType.addListener(observable -> Config.getInstance().set("loop_type",String.valueOf(LoopType.toInt(loopType.get()))));

        // 初始化数据库
        List<Music> l = DB.getInstance().getList(1);
        if (l == null) DB.getInstance().createList("play_list");
        else musicList.setAll(l);
        musicList.addListener((InvalidationListener) observable -> new Thread(() -> DB.getInstance().setList(1, musicList)).start());
    }

    public void playPref() {
        switch (loopType.get()) {
            case LIST -> {
                if (musicList.isEmpty()) return;
                Music playingMusic = Player.getInstance().playingMusicProperty().get();
                if (playingMusic != null) {
                    for (int i = 0; i < musicList.size(); i++) {
                        if (musicList.get(i).getMid().equals(playingMusic.getMid())) {
                            play(musicList.get((i + musicList.size() - 1) % musicList.size()));
                            return;
                        }
                    }
                }
                play(musicList.get(0));
            }
            case SINGLE -> Player.getInstance().play();
            case RANDOM -> {
                if (musicList.isEmpty()) return;
                if (history.isEmpty()) play(musicList.get((int) (Math.random() * musicList.size())));
                else {
                    history.removeLast();
                    if (!history.isEmpty()) {
                        var m = history.getLast();
                        history.removeLast();
                        play(m);
                    } else play(musicList.get((int) (Math.random() * musicList.size())));
                }
            }
        }
    }

    public void playNext() {
        switch (loopType.get()) {
            case LIST -> {
                if (musicList.isEmpty()) return;
                Music playingMusic = Player.getInstance().playingMusicProperty().get();
                if (playingMusic != null) {
                    for (int i = 0; i < musicList.size(); i++) {
                        if (musicList.get(i).getMid().equals(playingMusic.getMid())) {
                            play(musicList.get((i + 1) % musicList.size()));
                            return;
                        }
                    }
                }

                play(musicList.get(0));
            }
            case SINGLE -> Player.getInstance().play();
            case RANDOM -> {
                if (musicList.isEmpty()) return;
                play(musicList.get((int) (Math.random() * musicList.size())));
            }
        }
    }

    public void play(Music music) {
        System.out.println("Play:" + music);
        history.addLast(music);
        if (history.size() > historyMax) history.removeFirst();

        if (music.getType() == Music.Type.MUSIC) {
            add(music, false);
            Player.getInstance().play(music);
        } else {
            if (loading) return;
            loading = true;
            new Thread(new Task<Void>() {
                List<Music> l;

                @Override
                protected Void call() {
                    l = music.unfold();
                    return null;
                }

                @Override
                protected void succeeded() {
                    if (l != null) {
                        clear();
                        musicList.addAll(l);
                        if (!musicList.isEmpty()) Player.getInstance().play(musicList.get(0));
                    }
                    loading = false;
                }
            }).start();
        }
    }

    public void add(Music music, boolean msg) {
        if (music.getType() == Music.Type.MUSIC) {
            for (Music value : musicList) {
                if (value.getMid().equals(music.getMid())) {
                    if (msg) Config.getInstance().setMsg("已在列表中啦");
                    return;
                }
            }
            musicList.add(music);
            if (msg) Config.getInstance().setMsg("添加成功OvO");
        } else {
            if (loading) return;
            loading = true;
            new Thread(new Task<Void>() {
                List<Music> l;

                @Override
                protected Void call() {
                    l = music.unfold();
                    return null;
                }

                @Override
                protected void succeeded() {
                    if (l != null) {
                        Set<String> set = new HashSet<>();
                        musicList.forEach(m -> set.add(m.getMid()));
                        l.forEach(m -> {
                            if (!set.contains(m.getMid())) musicList.add(m);
                            set.add(m.getMid());
                        });
                        if (msg) Config.getInstance().setMsg("添加成功OvO");
                    } else if (msg) Config.getInstance().setMsg("添加失败Orz");
                    loading = false;
                }
            }).start();
        }
    }

    void delete(Music music) {
        for (int i = musicList.size() - 1; i >= 0; i--) {
            if (musicList.get(i).getMid().equals(music.getMid())) {
                musicList.remove(i);
                return;
            }
        }
    }

    public void clear() {
        if (musicList.isEmpty()) return;
        musicListHistory.addLast(musicList.stream().toList());
        while (musicListHistory.size() > musicListHistoryMax) musicListHistory.removeFirst();
        musicList.clear();
    }

    public void back() {
        if (musicListHistory.isEmpty()) return;
        musicList.setAll(musicListHistory.getLast());
        musicListHistory.removeLast();
    }

    @Override
    List<Operation> getOperations(Music music) {
        List<Operation> operations = new ArrayList<>();
        operations.add(new MusicTable.Operation("mdrmz-play_arrow", "播放", () -> play(music)));
        operations.add(new MusicTable.Operation("mdral-delete", "删除", () -> delete(music)));
        return operations;
    }

    public static PlayTable getInstance() {
        if (instance == null) instance = new PlayTable();
        return instance;
    }
}
