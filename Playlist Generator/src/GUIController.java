import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class GUIController extends Application {

    private TextField genreField;
    private TextField artistField;
    private TextField tagsField;

    private Button generateButton;
    private Button playButton;
    private Button pauseButton;
    private Button skipButton;

    private ListView<String> playlistView;
    private ObservableList<String> playlistItems;

    private Label nowPlayingLabel;
    private Label statusLabel;

    private PlaylistGenerator playlistGenerator;
    private MusicPlayer musicPlayer;
    private List<Song> allSongs;
    private List<Song> currentPlaylistSongs = new ArrayList<>();
    private Song currentSong;
    private Playlist currentPlaylist;

    private static final String BG_DEEP     = "#030008";
    private static final String BG_PANEL    = "#0a0015";
    private static final String BG_CARD     = "#0f001f";
    private static final String NEON_PINK   = "#ff2d78";
    private static final String NEON_PURPLE = "#bf00ff";
    private static final String NEON_CYAN   = "#00f5ff";
    private static final String NEON_YELLOW = "#f5ff00";
    private static final String TEXT_PRIMARY = "#f0e6ff";
    private static final String TEXT_DIM    = "#6a4a8a";
    private static final String BORDER_DIM  = "#2a0a3a";

    @Override
    public void start(Stage stage) {
        initBackend();

        VBox inputPanel    = buildInputPanel();
        VBox playlistPanel = buildPlaylistPanel();

        HBox mainContent = new HBox(0, inputPanel, playlistPanel);
        HBox.setHgrow(playlistPanel, Priority.ALWAYS);

        HBox header = buildHeader();

        VBox root = new VBox(0, header, mainContent);
        root.setStyle("-fx-background-color: " + BG_DEEP + ";");
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        connectEventHandlers();

        Scene scene = new Scene(root, 920, 620);
        scene.setFill(Color.web(BG_DEEP));

        stage.setTitle("Music Playlist Generator");
        stage.setScene(scene);
        stage.setMinWidth(780);
        stage.setMinHeight(520);
        stage.show();
    }

    private void initBackend() {
        SongDatabaseLoader loader = new SongDatabaseLoader();
        allSongs = loader.loadSongs("Playlist Generator/src/songs.json");
        playlistGenerator = new PlaylistGenerator(allSongs);
        musicPlayer = new MusicPlayer();
    }

    private HBox buildHeader() {
        Label appTitle = new Label("// MUSIC_PLAYLIST.GEN");
        appTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        appTitle.setStyle("-fx-text-fill: " + NEON_CYAN + ";");
        DropShadow titleGlow = new DropShadow();
        titleGlow.setColor(Color.web(NEON_CYAN));
        titleGlow.setRadius(18); titleGlow.setSpread(0.3);
        appTitle.setEffect(titleGlow);

        nowPlayingLabel = new Label("[ NO SIGNAL ]");
        nowPlayingLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        nowPlayingLabel.setStyle("-fx-text-fill: " + TEXT_DIM + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int count = (allSongs != null) ? allSongs.size() : 0;
        Label dbBadge = new Label("* " + count + " TRACKS LOADED");
        dbBadge.setFont(Font.font("Monospace", 10));
        dbBadge.setStyle(
            "-fx-text-fill: " + NEON_PURPLE + ";" +
            "-fx-background-color: #1a003a;" +
            "-fx-padding: 5 12 5 12;" +
            "-fx-background-radius: 2;" +
            "-fx-border-color: " + NEON_PURPLE + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 2;"
        );
        DropShadow badgeGlow = new DropShadow();
        badgeGlow.setColor(Color.web(NEON_PURPLE)); badgeGlow.setRadius(10);
        dbBadge.setEffect(badgeGlow);

        HBox header = new HBox(16, appTitle, spacer, dbBadge, nowPlayingLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setStyle(
            "-fx-background-color: " + BG_PANEL + ";" +
            "-fx-border-color: " + NEON_PINK + ";" +
            "-fx-border-width: 0 0 2 0;"
        );
        DropShadow headerShadow = new DropShadow();
        headerShadow.setColor(Color.web(NEON_PINK));
        headerShadow.setRadius(20); headerShadow.setOffsetY(2);
        header.setEffect(headerShadow);
        return header;
    }

    private VBox buildInputPanel() {
        Label sectionTitle = new Label("> PREFERENCES_");
        sectionTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        sectionTitle.setStyle("-fx-text-fill: " + NEON_PINK + ";");
        DropShadow sg = new DropShadow();
        sg.setColor(Color.web(NEON_PINK)); sg.setRadius(10);
        sectionTitle.setEffect(sg);

        genreField  = createStyledTextField("genre.exe");
        artistField = createStyledTextField("artist.exe");
        tagsField   = createStyledTextField("tags[] comma-separated");

        VBox genreBox  = labeledField("[ GENRE ]",  genreField);
        VBox artistBox = labeledField("[ ARTIST ]", artistField);
        VBox tagsBox   = labeledField("[ TAGS ]",   tagsField);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + NEON_PINK + "; -fx-opacity: 0.3;");

        generateButton = new Button("RUN GENERATOR");
        generateButton.setMaxWidth(Double.MAX_VALUE);
        generateButton.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        styleGenBtn(false);
        generateButton.setOnMouseEntered(e -> styleGenBtn(true));
        generateButton.setOnMouseExited(e  -> styleGenBtn(false));

        statusLabel = new Label(allSongs != null && !allSongs.isEmpty()
            ? "[ OK ] " + allSongs.size() + " songs indexed"
            : "[ ERR ] songs.json not found");
        statusLabel.setFont(Font.font("Monospace", 10));
        statusLabel.setStyle(
            (allSongs != null && !allSongs.isEmpty())
                ? "-fx-text-fill: " + NEON_CYAN + ";"
                : "-fx-text-fill: " + NEON_PINK + ";"
        );
        statusLabel.setWrapText(true);

        VBox panel = new VBox(14,
            sectionTitle, genreBox, artistBox, tagsBox,
            sep, generateButton, statusLabel
        );
        panel.setPadding(new Insets(22, 18, 22, 22));
        panel.setPrefWidth(290);
        panel.setMinWidth(260);
        panel.setStyle(
            "-fx-background-color: " + BG_PANEL + ";" +
            "-fx-border-color: " + NEON_PINK + ";" +
            "-fx-border-width: 0 2 0 0;"
        );
        return panel;
    }

    private VBox buildPlaylistPanel() {
        Label sectionTitle = new Label("> PLAYLIST_OUTPUT_");
        sectionTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        sectionTitle.setStyle("-fx-text-fill: " + NEON_CYAN + ";");
        DropShadow cg = new DropShadow();
        cg.setColor(Color.web(NEON_CYAN)); cg.setRadius(10);
        sectionTitle.setEffect(cg);

        playlistItems = FXCollections.observableArrayList();
        playlistView  = new ListView<>(playlistItems);
        VBox.setVgrow(playlistView, Priority.ALWAYS);
        playlistView.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-background-insets: 0;" +
            "-fx-border-color: " + NEON_PURPLE + ";" +
            "-fx-border-radius: 2;" +
            "-fx-background-radius: 2;" +
            "-fx-padding: 4;"
        );
        DropShadow listGlow = new DropShadow();
        listGlow.setColor(Color.web(NEON_PURPLE));
        listGlow.setRadius(15); listGlow.setSpread(0.05);
        playlistView.setEffect(listGlow);

        Label placeholder = new Label("// awaiting input...");
        placeholder.setFont(Font.font("Monospace", 13));
        placeholder.setStyle("-fx-text-fill: " + TEXT_DIM + ";");
        playlistView.setPlaceholder(placeholder);

        playlistView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    if (isSelected()) {
                        setStyle(
                            "-fx-text-fill: " + NEON_YELLOW + ";" +
                            "-fx-font-family: Monospace;" +
                            "-fx-font-size: 13px;" +
                            "-fx-padding: 10 14 10 14;" +
                            "-fx-background-color: #1f003f;" +
                            "-fx-border-color: " + NEON_PINK + " transparent " + NEON_PINK + " transparent;" +
                            "-fx-border-width: 1 0 1 0;"
                        );
                    } else {
                        setStyle(
                            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                            "-fx-font-family: Monospace;" +
                            "-fx-font-size: 13px;" +
                            "-fx-padding: 10 14 10 14;" +
                            "-fx-background-color: transparent;" +
                            "-fx-border-color: transparent transparent " + BORDER_DIM + " transparent;" +
                            "-fx-border-width: 0 0 1 0;"
                        );
                    }
                }
            }
        });

        playlistView.getSelectionModel().selectedIndexProperty().addListener(
            (obs, o, n) -> playlistView.refresh()
        );

        HBox playerControls = buildPlayerControls();

        VBox panel = new VBox(14, sectionTitle, playlistView, playerControls);
        panel.setPadding(new Insets(22, 22, 22, 16));
        panel.setStyle("-fx-background-color: " + BG_DEEP + ";");
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private HBox buildPlayerControls() {
    	playButton  = createPlayerButton("PLAY",  "#00ff44");
    	pauseButton = createPlayerButton("PAUSE", "#ff6600");
    	skipButton  = createPlayerButton("SKIP",  "#ffd700");

        playButton.setDisable(true);
        pauseButton.setDisable(true);
        skipButton.setDisable(true);

        HBox row = new HBox(12, playButton, pauseButton, skipButton);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(8, 0, 0, 0));
        return row;
    }

    private void connectEventHandlers() {
        generateButton.setOnAction(e -> handleGeneratePlaylist());
        playButton.setOnAction(e     -> handlePlay());
        pauseButton.setOnAction(e    -> handlePause());
        skipButton.setOnAction(e     -> handleSkip());
        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) handlePlay();
        });
    }

    private void handleGeneratePlaylist() {
        String genre  = genreField.getText().trim();
        String artist = artistField.getText().trim();
        String tags   = tagsField.getText().trim();

        if (genre.isEmpty() && artist.isEmpty() && tags.isEmpty()) {
            statusLabel.setText("[ ERR ] input required");
            statusLabel.setStyle("-fx-text-fill: " + NEON_PINK + "; -fx-font-family: Monospace; -fx-font-size: 11px;");
            return;
        }

        statusLabel.setText("[ ... ] scanning database");
        statusLabel.setStyle("-fx-text-fill: " + NEON_YELLOW + "; -fx-font-family: Monospace; -fx-font-size: 11px;");
        playlistItems.clear();
        currentPlaylistSongs.clear();

        List<String> keywords = new ArrayList<>();
        if (!tags.isEmpty()) {
            for (String tag : tags.split(",")) {
                String c = tag.trim();
                if (!c.isEmpty()) keywords.add(c);
            }
        }

        UserPreferences prefs = new UserPreferences(genre, artist, keywords);
        currentPlaylist       = playlistGenerator.generate(prefs);
        List<Song> songs      = currentPlaylist.getSongs();

        if (songs.isEmpty()) {
            statusLabel.setText("[ 0 ] no matches found");
            statusLabel.setStyle("-fx-text-fill: " + NEON_PINK + "; -fx-font-family: Monospace; -fx-font-size: 11px;");
            return;
        }

        for (int i = 0; i < songs.size(); i++) {
            playlistItems.add((i + 1) + ".  " + songs.get(i).toString());
            currentPlaylistSongs.add(songs.get(i));
        }

        statusLabel.setText("[ OK ] " + songs.size() + " track" + (songs.size() == 1 ? "" : "s") + " found");
        statusLabel.setStyle("-fx-text-fill: " + NEON_CYAN + "; -fx-font-family: Monospace; -fx-font-size: 11px;");

        playButton.setDisable(false);
        pauseButton.setDisable(false);
        skipButton.setDisable(false);

        playlistView.getSelectionModel().selectFirst();
        musicPlayer.setPlaylist(currentPlaylist);
    }

    private void handlePlay() {
        int idx = playlistView.getSelectionModel().getSelectedIndex();
        if (idx < 0 && !currentPlaylistSongs.isEmpty()) {
            idx = 0;
            playlistView.getSelectionModel().select(0);
        }
        if (idx >= 0 && idx < currentPlaylistSongs.size()) {
            currentSong = currentPlaylistSongs.get(idx);
            nowPlayingLabel.setText("NOW PLAYING: " + currentSong.toString());
            nowPlayingLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
            nowPlayingLabel.setStyle("-fx-text-fill: " + NEON_CYAN + ";");
            DropShadow ng = new DropShadow();
            ng.setColor(Color.web(NEON_CYAN)); ng.setRadius(12);
            nowPlayingLabel.setEffect(ng);
            musicPlayer.play();
        }
    }

    private void handlePause() {
        nowPlayingLabel.setText("PAUSED");
        nowPlayingLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        nowPlayingLabel.setStyle("-fx-text-fill: " + NEON_YELLOW + ";");
        nowPlayingLabel.setEffect(null);
        musicPlayer.pause();
    }

    private void handleSkip() {
        musicPlayer.skip();
        int current = playlistView.getSelectionModel().getSelectedIndex();
        if (current < 0) current = 0;
        int next = (current + 1) % currentPlaylistSongs.size();
        playlistView.getSelectionModel().select(next);
        currentSong = currentPlaylistSongs.get(next);
        nowPlayingLabel.setText("NOW PLAYING: " + currentSong.toString());
        nowPlayingLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        nowPlayingLabel.setStyle("-fx-text-fill: " + NEON_CYAN + ";");
        DropShadow ng = new DropShadow();
        ng.setColor(Color.web(NEON_CYAN)); ng.setRadius(12);
        nowPlayingLabel.setEffect(ng);
    }

    private void styleGenBtn(boolean hover) {
        generateButton.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        generateButton.setStyle(
            "-fx-background-color: " + (hover ? NEON_PINK : "transparent") + ";" +
            "-fx-text-fill: " + (hover ? "#000000" : NEON_PINK) + ";" +
            "-fx-padding: 13 20 13 20;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 2;" +
            "-fx-border-color: " + NEON_PINK + ";" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 2;"
        );
        if (hover) {
            DropShadow g = new DropShadow();
            g.setColor(Color.web(NEON_PINK)); g.setRadius(20); g.setSpread(0.2);
            generateButton.setEffect(g);
        } else {
            generateButton.setEffect(null);
        }
    }

    private TextField createStyledTextField(String placeholder) {
        TextField field = new TextField();
        field.setPromptText(placeholder);
        applyFieldStyle(field, false);
        field.focusedProperty().addListener((obs, w, isFocused) -> applyFieldStyle(field, isFocused));
        return field;
    }

    private void applyFieldStyle(TextField field, boolean focused) {
        field.setFont(Font.font("Monospace", 12));
        field.setStyle(
            "-fx-background-color: #0a0018;" +
            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
            "-fx-prompt-text-fill: " + TEXT_DIM + ";" +
            "-fx-padding: 9 12 9 12;" +
            "-fx-background-radius: 2;" +
            "-fx-border-radius: 2;" +
            (focused
                ? "-fx-border-color: " + NEON_CYAN + "; -fx-border-width: 1.5;"
                : "-fx-border-color: " + BORDER_DIM + "; -fx-border-width: 1;")
        );
        if (focused) {
            DropShadow g = new DropShadow();
            g.setColor(Color.web(NEON_CYAN)); g.setRadius(12); g.setSpread(0.1);
            field.setEffect(g);
        } else {
            field.setEffect(null);
        }
    }

    private VBox labeledField(String caption, TextField field) {
        Label lbl = new Label(caption);
        lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        lbl.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-letter-spacing: 1.5px;");
        return new VBox(5, lbl, field);
    }

    private Button createPlayerButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(145);
        btn.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        applyPlayerStyle(btn, color, false);
        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) applyPlayerStyle(btn, color, true);  });
        btn.setOnMouseExited(e  -> { if (!btn.isDisabled()) applyPlayerStyle(btn, color, false); });
        return btn;
    }

    private void applyPlayerStyle(Button btn, String color, boolean hover) {
        btn.setStyle(
            "-fx-background-color: " + (hover ? color : "transparent") + ";" +
            "-fx-text-fill: " + (hover ? "#000000" : color) + ";" +
            "-fx-padding: 11 15 11 15;" +
            "-fx-background-radius: 2;" +
            "-fx-cursor: hand;" +
            "-fx-border-radius: 2;" +
            "-fx-border-width: 2;" +
            "-fx-border-color: " + color + ";"
        );
        if (hover) {
            DropShadow g = new DropShadow();
            g.setColor(Color.web(color)); g.setRadius(18); g.setSpread(0.15);
            btn.setEffect(g);
        } else {
            DropShadow g = new DropShadow();
            g.setColor(Color.web(color)); g.setRadius(8); g.setSpread(0.05);
            btn.setEffect(g);
        }
    }
}