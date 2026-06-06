import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    private TextField moodField;
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
    private List<Song> allSongs;
    private List<Song> currentPlaylistSongs = new ArrayList<>();
    private Song currentSong;

    // private MusicPlayer musicPlayer;

    @Override
    public void start(Stage stage) {
        initBackend();

        VBox inputPanel    = buildInputPanel();
        VBox playlistPanel = buildPlaylistPanel();

        HBox mainContent = new HBox(0, inputPanel, playlistPanel);
        HBox.setHgrow(playlistPanel, Priority.ALWAYS);

        HBox header = buildHeader();

        VBox root = new VBox(0, header, mainContent);
        root.setStyle("-fx-background-color: #0d0d0d;");
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        connectEventHandlers();

        Scene scene = new Scene(root, 920, 660);
        scene.setFill(Color.web("#0d0d0d"));

        stage.setTitle("Music Playlist Generator");
        stage.setScene(scene);
        stage.setMinWidth(780);
        stage.setMinHeight(560);
        stage.show();
    }

    private void initBackend() {
        SongDatabaseLoader loader = new SongDatabaseLoader();
        allSongs = loader.loadSongs("Playlist Generator/src/songs.json");
        playlistGenerator = new PlaylistGenerator(allSongs);
        // musicPlayer = new MusicPlayer();
    }

    private HBox buildHeader() {
        Label appTitle = new Label("♫  MUSIC PLAYLIST GENERATOR");
        appTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        appTitle.setStyle("-fx-text-fill: #ffffff; -fx-letter-spacing: 2px;");

        nowPlayingLabel = new Label("No song playing");
        nowPlayingLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int count = (allSongs != null) ? allSongs.size() : 0;
        Label dbBadge = new Label(count + " songs in library");
        dbBadge.setStyle(
            "-fx-text-fill: #444444;" +
            "-fx-font-size: 11px;" +
            "-fx-background-color: #1a1a1a;" +
            "-fx-padding: 4 10 4 10;" +
            "-fx-background-radius: 20;"
        );

        HBox header = new HBox(16, appTitle, spacer, dbBadge, nowPlayingLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle(
            "-fx-background-color: #141414;" +
            "-fx-border-color: #252525;" +
            "-fx-border-width: 0 0 1 0;"
        );
        return header;
    }

    private VBox buildInputPanel() {
        Label sectionTitle = new Label("PREFERENCES");
        sectionTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        sectionTitle.setStyle("-fx-text-fill: #555555; -fx-letter-spacing: 2px;");

        genreField  = createStyledTextField("e.g.  Pop,  Rock,  Jazz");
        artistField = createStyledTextField("e.g.  Adele,  Eminem");
        moodField   = createStyledTextField("e.g.  calm,  energetic");
        tagsField   = createStyledTextField("e.g.  study,  workout  (comma-separated)");

        VBox genreBox  = labeledField("GENRE",  genreField);
        VBox artistBox = labeledField("ARTIST", artistField);
        VBox moodBox   = labeledField("MOOD",   moodField);
        VBox tagsBox   = labeledField("TAGS",   tagsField);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #252525;");

        generateButton = new Button("GENERATE PLAYLIST");
        generateButton.setMaxWidth(Double.MAX_VALUE);
        generateButton.setStyle(
            "-fx-background-color: #1db954;" +
            "-fx-text-fill: #000000;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 13 20 13 20;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 7;" +
            "-fx-letter-spacing: 1px;"
        );
        generateButton.setOnMouseEntered(e -> generateButton.setStyle(
            "-fx-background-color: #1ed760;" +
            "-fx-text-fill: #000000;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 13 20 13 20;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 7;" +
            "-fx-letter-spacing: 1px;"
        ));
        generateButton.setOnMouseExited(e -> generateButton.setStyle(
            "-fx-background-color: #1db954;" +
            "-fx-text-fill: #000000;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 13 20 13 20;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 7;" +
            "-fx-letter-spacing: 1px;"
        ));

        statusLabel = new Label(allSongs != null && !allSongs.isEmpty()
            ? "✓ Library loaded — " + allSongs.size() + " songs ready"
            : "⚠ songs.json not found — check file path");
        statusLabel.setStyle(
            (allSongs != null && !allSongs.isEmpty())
                ? "-fx-text-fill: #3a7a4a; -fx-font-size: 11px;"
                : "-fx-text-fill: #884444; -fx-font-size: 11px;"
        );
        statusLabel.setWrapText(true);

        VBox panel = new VBox(14,
            sectionTitle, genreBox, artistBox, moodBox, tagsBox,
            sep, generateButton, statusLabel
        );
        panel.setPadding(new Insets(24, 20, 24, 24));
        panel.setPrefWidth(285);
        panel.setMinWidth(255);
        panel.setStyle("-fx-background-color: #111111;");
        return panel;
    }

    private VBox buildPlaylistPanel() {
        Label sectionTitle = new Label("GENERATED PLAYLIST");
        sectionTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        sectionTitle.setStyle("-fx-text-fill: #555555; -fx-letter-spacing: 2px;");

        playlistItems = FXCollections.observableArrayList();
        playlistView  = new ListView<>(playlistItems);
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        playlistView.setStyle(
            "-fx-background-color: #0d0d0d;" +
            "-fx-background-insets: 0;" +
            "-fx-border-color: #222222;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 4;"
        );

        Label placeholder = new Label("Set your preferences and click Generate");
        placeholder.setStyle("-fx-text-fill: #3a3a3a; -fx-font-size: 13px;");
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
                            "-fx-text-fill: #ffffff;" +
                            "-fx-font-size: 13px;" +
                            "-fx-padding: 10 14 10 14;" +
                            "-fx-background-color: #1a2e1a;" +
                            "-fx-border-color: transparent transparent #1db954 transparent;" +
                            "-fx-border-width: 0 0 1 0;"
                        );
                    } else {
                        setStyle(
                            "-fx-text-fill: #cccccc;" +
                            "-fx-font-size: 13px;" +
                            "-fx-padding: 10 14 10 14;" +
                            "-fx-background-color: transparent;" +
                            "-fx-border-color: transparent transparent #1e1e1e transparent;" +
                            "-fx-border-width: 0 0 1 0;"
                        );
                    }
                }
            }
        });

        playlistView.getSelectionModel().selectedIndexProperty().addListener(
            (obs, oldVal, newVal) -> playlistView.refresh()
        );

        HBox playerControls = buildPlayerControls();

        VBox panel = new VBox(14, sectionTitle, playlistView, playerControls);
        panel.setPadding(new Insets(24, 24, 24, 16));
        panel.setStyle("-fx-background-color: #0d0d0d;");
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private HBox buildPlayerControls() {
        playButton  = createPlayerButton("▶   PLAY");
        pauseButton = createPlayerButton("⏸   PAUSE");
        skipButton  = createPlayerButton("⏭   SKIP");

        playButton.setDisable(true);
        pauseButton.setDisable(true);
        skipButton.setDisable(true);

        HBox row = new HBox(10, playButton, pauseButton, skipButton);
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
        String mood   = moodField.getText().trim();
        String tags   = tagsField.getText().trim();

        if (genre.isEmpty() && artist.isEmpty() && mood.isEmpty() && tags.isEmpty()) {
            statusLabel.setText("⚠  Enter at least one preference to search.");
            statusLabel.setStyle("-fx-text-fill: #c05050; -fx-font-size: 11px;");
            return;
        }

        statusLabel.setText("Searching library...");
        statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        playlistItems.clear();
        currentPlaylistSongs.clear();

        List<String> keywords = new ArrayList<>();
        if (!mood.isEmpty()) keywords.add(mood.trim());
        if (!tags.isEmpty()) {
            for (String tag : tags.split(",")) {
                String cleaned = tag.trim();
                if (!cleaned.isEmpty()) keywords.add(cleaned);
            }
        }

        UserPreferences prefs = new UserPreferences(genre, artist, keywords);
        Playlist playlist     = playlistGenerator.generate(prefs);
        List<Song> songs      = playlist.getSongs();

        if (songs.isEmpty()) {
            statusLabel.setText("No songs matched — try different preferences.");
            statusLabel.setStyle("-fx-text-fill: #c07030; -fx-font-size: 11px;");
            return;
        }

        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            playlistItems.add((i + 1) + ".   " + song.toString());
            currentPlaylistSongs.add(song);
        }

        statusLabel.setText("✓  " + songs.size() + " song" + (songs.size() == 1 ? "" : "s") + " found");
        statusLabel.setStyle("-fx-text-fill: #1db954; -fx-font-size: 11px;");

        playButton.setDisable(false);
        pauseButton.setDisable(false);
        skipButton.setDisable(false);

        playlistView.getSelectionModel().selectFirst();
    }

    private void handlePlay() {
        int selectedIndex = playlistView.getSelectionModel().getSelectedIndex();

        if (selectedIndex < 0 && !currentPlaylistSongs.isEmpty()) {
            selectedIndex = 0;
            playlistView.getSelectionModel().select(0);
        }

        if (selectedIndex >= 0 && selectedIndex < currentPlaylistSongs.size()) {
            currentSong = currentPlaylistSongs.get(selectedIndex);
            nowPlayingLabel.setText("▶   " + currentSong.toString());
            nowPlayingLabel.setStyle("-fx-text-fill: #1db954; -fx-font-size: 12px;");
            // musicPlayer.play(currentSong);
        }
    }

    private void handlePause() {
        nowPlayingLabel.setText("⏸   Paused");
        nowPlayingLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
        // musicPlayer.pause();
    }

    private void handleSkip() {
        int current = playlistView.getSelectionModel().getSelectedIndex();
        if (current < 0) current = 0;
        int next = (current + 1) % currentPlaylistSongs.size();
        playlistView.getSelectionModel().select(next);
        // musicPlayer.skip();
        handlePlay();
    }

    private TextField createStyledTextField(String placeholder) {
        TextField field = new TextField();
        field.setPromptText(placeholder);
        applyFieldStyle(field, false);
        field.focusedProperty().addListener((obs, wasF, isF) -> applyFieldStyle(field, isF));
        return field;
    }

    private void applyFieldStyle(TextField field, boolean focused) {
        field.setStyle(
            "-fx-background-color: #181818;" +
            "-fx-text-fill: #eeeeee;" +
            "-fx-prompt-text-fill: #444444;" +
            "-fx-padding: 9 12 9 12;" +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 6;" +
            "-fx-font-size: 13px;" +
            (focused
                ? "-fx-border-color: #1db954; -fx-border-width: 1.5;"
                : "-fx-border-color: #2a2a2a; -fx-border-width: 1;")
        );
    }

    private VBox labeledField(String caption, TextField field) {
        Label lbl = new Label(caption);
        lbl.setFont(Font.font("Monospace", 9));
        lbl.setStyle("-fx-text-fill: #484848; -fx-letter-spacing: 1.5px;");
        return new VBox(5, lbl, field);
    }

    private Button createPlayerButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(135);
        applyPlayerButtonStyle(btn, false);
        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) applyPlayerButtonStyle(btn, true);  });
        btn.setOnMouseExited(e  -> { if (!btn.isDisabled()) applyPlayerButtonStyle(btn, false); });
        return btn;
    }

    private void applyPlayerButtonStyle(Button btn, boolean hover) {
        btn.setStyle(
            "-fx-background-color: " + (hover ? "#1e1e1e" : "#161616") + ";" +
            "-fx-text-fill: " + (hover ? "#ffffff" : "#aaaaaa") + ";" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 15 10 15;" +
            "-fx-background-radius: 7;" +
            "-fx-cursor: hand;" +
            "-fx-border-radius: 7;" +
            "-fx-border-width: 1;" +
            (hover ? "-fx-border-color: #1db954;" : "-fx-border-color: #252525;")
        );
    }
}