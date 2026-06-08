import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main application class — handles everything related to the UI.
 * Extends JavaFX Application so the framework calls start() automatically on launch.
 *
 * All the actual music logic (filtering, playback) lives in PlaylistGenerator and MusicPlayer.
 * This class just builds the interface, listens for user input, and calls those classes.
 */
public class GUIController extends Application {

    // user input fields
    private TextField genreField;
    private TextField artistField;
    private TextField tagsField;

    // buttons — playback controls are disabled until a playlist is generated
    private Button generateButton;
    private Button playButton;
    private Button pauseButton;
    private Button skipButton;

    // ObservableList is used here because it automatically notifies the ListView
    // whenever items are added or removed — no need to call refresh() manually
    private ListView<String> playlistView;
    private ObservableList<String> playlistItems;
    private List<Song> currentPlaylistSongs = new ArrayList<>();

    private Label nowPlayingLabel;
    private Label statusLabel;

    // backend objects — all music logic is handled by these, not by this class
    private PlaylistGenerator playlistGenerator;
    private MusicPlayer musicPlayer;
    private List<Song> allSongs;      // loaded from songs.json on startup
    private Song currentSong;
    private Playlist currentPlaylist;

    // colour constants used in all the inline JavaFX CSS throughout the file
    private static final String DEEP_SPACE  = "#050012";
    private static final String STAR_WHITE  = "#ece6ff";
    private static final String PURP_NEON   = "#a855f7"; // playlist panel accent
    private static final String CYAN_SOFT   = "#22d3ee"; // input panel accent
    private static final String PINK_SOFT   = "#f472b6"; // pause button
    private static final String GOLD        = "#fbbf24"; // now-playing / skip
    private static final String DIM         = "#2e1a4a";
    private static final String TEXT_MUTED  = "#6b4f8a";

    // parallel arrays for the starfield — each index is one star
    private double[] starX, starY, starSize, starOpacity;
    private Timeline starTwinkle;
    private Random rnd = new Random();

    /**
     * JavaFX calls this automatically when the app starts.
     * We set up the backend first, then build the UI on top of the animated canvas.
     */
    @Override
    public void start(Stage stage) {
        initBackend();
        initStars(940, 680);

        // StackPane lets us layer the canvas behind the UI — canvas goes in first
        StackPane root = new StackPane();
        Canvas starCanvas = new Canvas(940, 680);
        drawStarfield(starCanvas.getGraphicsContext2D(), 940, 680);

        // Timeline runs indefinitely, updating a few star opacities every 100ms
        starTwinkle = new Timeline(new KeyFrame(Duration.millis(100), e ->
            twinkleStars(starCanvas.getGraphicsContext2D(), 940, 680)
        ));
        starTwinkle.setCycleCount(Animation.INDEFINITE);
        starTwinkle.play();

        VBox ui = buildUI();
        root.getChildren().addAll(starCanvas, ui); // canvas added first so it renders behind

        Scene scene = new Scene(root, 940, 680);
        scene.setFill(Color.web(DEEP_SPACE));
        stage.setTitle("Music Playlist Generator");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(580);
        stage.show();
    }

    /**
     * Loads the song database and sets up the backend objects.
     * Wrapped in try-catch so a missing or broken JSON file doesn't crash the app —
     * it just starts with an empty song list instead.
     */
    private void initBackend() {
        try {
            SongDatabaseLoader loader = new SongDatabaseLoader();
            // classpath resource so the path works regardless of where the project is opened
            String path = getClass().getResource("/songs.json").toExternalForm().replace("file:", "");
            allSongs = loader.loadSongs(path);
            playlistGenerator = new PlaylistGenerator(allSongs);
            musicPlayer = new MusicPlayer();
        } catch (Exception e) {
            allSongs = new ArrayList<>();
            System.err.println("Failed to load song database: " + e.getMessage());
        }
    }

    // Fills the star arrays with random positions, sizes, and starting opacities.
    // Using parallel arrays instead of a Star class keeps the drawing loop simple.
    private void initStars(double w, double h) {
        int count = 160;
        starX = new double[count]; starY = new double[count];
        starSize = new double[count]; starOpacity = new double[count];
        for (int i = 0; i < count; i++) {
            starX[i] = rnd.nextDouble() * w;
            starY[i] = rnd.nextDouble() * h;
            starSize[i] = 0.5 + rnd.nextDouble() * 1.8;    // keep stars small (0.5–2.3 px)
            starOpacity[i] = 0.2 + rnd.nextDouble() * 0.6; // vary brightness at start
        }
    }

    // Redraws the full background: base fill, three soft nebula ovals for depth, then the stars.
    // Called once on startup and again every frame during the twinkling animation.
    private void drawStarfield(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.web(DEEP_SPACE));
        gc.fillRect(0, 0, w, h);
        // semi-transparent ovals give the background some depth without hiding the stars
        gc.setFill(Color.web("#1a0840", 0.5));
        gc.fillOval(w * 0.25, h * 0.1, w * 0.55, h * 0.8);
        gc.setFill(Color.web("#0a1030", 0.3));
        gc.fillOval(0, h * 0.3, w * 0.4, h * 0.5);
        gc.setFill(Color.web("#200830", 0.2));
        gc.fillOval(w * 0.6, 0, w * 0.4, h * 0.5);
        drawStars(gc);
    }

    // Draws every star using its current opacity — called at the end of each frame.
    private void drawStars(GraphicsContext gc) {
        for (int i = 0; i < starX.length; i++) {
            gc.setFill(Color.web(STAR_WHITE, starOpacity[i]));
            gc.fillOval(starX[i], starY[i], starSize[i], starSize[i]);
        }
    }

    // Called every 100ms by the Timeline. Redraws the whole background, then randomly
    // shifts the opacity of 8 stars to fake a twinkling effect.
    private void twinkleStars(GraphicsContext gc, double w, double h) {
        drawStarfield(gc, w, h);
        for (int i = 0; i < 8; i++) {
            int idx = rnd.nextInt(starX.length);
            starOpacity[idx] = 0.15 + rnd.nextDouble() * 0.75;
        }
        drawStars(gc);
    }

    /**
     * Assembles the root UI layout — header on top, two panels in the middle, footer at the bottom.
     * connectEventHandlers() is called here after all components exist.
     */
    private VBox buildUI() {
        VBox inputPanel    = buildInputPanel();
        VBox playlistPanel = buildPlaylistPanel();

        // playlist panel grows horizontally to fill remaining space
        HBox mainContent = new HBox(16, inputPanel, playlistPanel);
        mainContent.setPadding(new Insets(0, 16, 0, 16));
        HBox.setHgrow(playlistPanel, Priority.ALWAYS);

        VBox header = buildHeader();
        HBox footer = buildFooter();

        VBox root = new VBox(0, header, mainContent, footer);
        root.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(mainContent, Priority.ALWAYS); // stretch to fill vertical space

        connectEventHandlers();
        return root;
    }

    // Builds the top banner: title with a glow effect, track count subtitle, and a gradient divider.
    private VBox buildHeader() {
    	
    	Label starLeft  = new Label("+");
        Label starRight = new Label("+");
        starLeft.setFont(Font.font("SansSerif", 14));
        starRight.setFont(Font.font("SansSerif", 14));
        starLeft.setStyle("-fx-text-fill: #ffffff;");
        starRight.setStyle("-fx-text-fill: #ffffff;");

        Label title = new Label("MUSIC PLAYLIST GENERATOR");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: " + STAR_WHITE + ";");

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(PURP_NEON, 0.7));
        glow.setRadius(22); glow.setSpread(0.2);
        title.setEffect(glow);

        HBox titleRow = new HBox(12, starLeft, title, starRight);
        titleRow.setAlignment(Pos.CENTER);

        // shows how many songs were actually loaded — useful for checking if JSON parsed correctly
        int count = (allSongs != null) ? allSongs.size() : 0;
        Label sub = new Label(count + " tracks in the universe");
        sub.setFont(Font.font("Monospace", 10));
        sub.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-letter-spacing: 2px;");

        VBox center = new VBox(6, titleRow, sub);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(20, 24, 16, 24));

        Pane line = new Pane();
        line.setPrefHeight(1);
        line.setStyle(
            "-fx-background-color: linear-gradient(to right, " +
            PURP_NEON + ", " + CYAN_SOFT + ", " + PINK_SOFT + ");" +
            "-fx-opacity: 0.5;"
        );

        VBox header = new VBox(0, center, line);
        header.setStyle("-fx-background-color: transparent;");
        return header;
    }

    // Footer shows what's currently playing on the left and the group name on the right.
    private HBox buildFooter() {
        nowPlayingLabel = new Label("No track selected");
        nowPlayingLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
        nowPlayingLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");

        // Region with ALWAYS priority pushes the tagline to the far right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tagline = new Label("Group 8  ·  2026");
        tagline.setFont(Font.font("SansSerif", 10));
        tagline.setStyle("-fx-text-fill: " + DIM + ";");

        Pane topLine = new Pane();
        topLine.setPrefHeight(1);
        topLine.setStyle(
            "-fx-background-color: linear-gradient(to right, " +
            PURP_NEON + ", " + CYAN_SOFT + ");" +
            "-fx-opacity: 0.2;"
        );

        HBox row = new HBox(10, nowPlayingLabel, spacer, tagline);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 20, 14, 20));

        VBox footer = new VBox(0, topLine, row);
        footer.setStyle("-fx-background-color: transparent;");
        HBox wrapper = new HBox(footer);
        HBox.setHgrow(footer, Priority.ALWAYS);
        return wrapper;
    }

    /**
     * Builds the left panel where the user enters their search preferences.
     * Shows a status label at the bottom reflecting how many songs loaded (or an error).
     */
    private VBox buildInputPanel() {
        Label sectionTitle = new Label("NAVIGATION");
        sectionTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        sectionTitle.setStyle("-fx-text-fill: " + CYAN_SOFT + "; -fx-letter-spacing: 3px;");
        DropShadow sg = new DropShadow();
        sg.setColor(Color.web(CYAN_SOFT, 0.4)); sg.setRadius(8);
        sectionTitle.setEffect(sg);

        genreField  = createField("e.g.  Pop,  Jazz,  Rock");
        artistField = createField("e.g.  Adele,  Eminem");
        tagsField   = createField("e.g.  calm,  workout");

        VBox genreBox  = fieldGroup("GENRE",  genreField);
        VBox artistBox = fieldGroup("ARTIST", artistField);
        VBox tagsBox   = fieldGroup("TAGS",   tagsField);

        generateButton = new Button("LAUNCH SEARCH");
        generateButton.setMaxWidth(Double.MAX_VALUE);
        generateButton.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        styleGenBtn(false);
        generateButton.setOnMouseEntered(e -> styleGenBtn(true));
        generateButton.setOnMouseExited(e  -> styleGenBtn(false));

        // red text if the database failed to load, cyan if everything is fine
        statusLabel = new Label(allSongs != null && !allSongs.isEmpty()
            ? allSongs.size() + " signals ready"
            : "Error: database not found");
        statusLabel.setFont(Font.font("Monospace", 10));
        statusLabel.setStyle(
            (allSongs != null && !allSongs.isEmpty())
                ? "-fx-text-fill: " + CYAN_SOFT + ";"
                : "-fx-text-fill: #f87171;"
        );
        statusLabel.setWrapText(true);

        VBox panel = new VBox(16,
            sectionTitle, genreBox, artistBox, tagsBox,
            generateButton, statusLabel
        );
        panel.setPadding(new Insets(24, 16, 24, 16));
        panel.setPrefWidth(280);
        panel.setMinWidth(250);
        // Semi-transparent dark card with a subtle cyan border
        panel.setStyle(
            "-fx-background-color: rgba(10,4,32,0.82);" +
            "-fx-border-color: rgba(34,211,238,0.2);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;" +
            "-fx-background-radius: 16;"
        );
        DropShadow panelGlow = new DropShadow();
        panelGlow.setColor(Color.web(CYAN_SOFT, 0.15));
        panelGlow.setRadius(24);
        panel.setEffect(panelGlow);

        VBox wrapper = new VBox(panel);
        wrapper.setPadding(new Insets(16, 0, 16, 0));
        return wrapper;
    }

    /**
     * Builds the right panel that shows the generated playlist and playback controls.
     * The ListView uses a custom CellFactory so each row displays the title and artist
     * on separate lines with different styling, rather than just a plain string.
     */
    private VBox buildPlaylistPanel() {
        Label sectionTitle = new Label("DISCOVERED TRACKS");
        sectionTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        sectionTitle.setStyle("-fx-text-fill: " + PURP_NEON + "; -fx-letter-spacing: 3px;");
        DropShadow cg = new DropShadow();
        cg.setColor(Color.web(PURP_NEON, 0.4)); cg.setRadius(8);
        sectionTitle.setEffect(cg);

        playlistItems = FXCollections.observableArrayList();
        playlistView  = new ListView<>(playlistItems);
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        playlistView.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background-insets: 0;" +
            "-fx-border-width: 0;" +
            "-fx-padding: 4;"
        );

        Label placeholder = new Label("Chart a course and launch your search");
        placeholder.setFont(Font.font("SansSerif", 12));
        placeholder.setStyle("-fx-text-fill: " + DIM + ";");
        playlistView.setPlaceholder(placeholder);

        // Custom cell factory — splits "Title - Artist [Album]" into two separate labels
        playlistView.setCellFactory(lv -> new ListCell<String>() {
            private final Label titleLbl  = new Label();
            private final Label artistLbl = new Label();
            private final VBox  cellBox   = new VBox(2, titleLbl, artistLbl);
            {
                // instance initializer runs once when the cell is first created
                cellBox.setPadding(new Insets(8, 14, 8, 14));
                titleLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
                artistLbl.setFont(Font.font("SansSerif", 11));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // strip the "1.   " numbering prefix before splitting on " - "
                    String clean  = item.replaceFirst("^\\d+\\.\\s*", "");
                    String[] parts = clean.split(" - ", 2);
                    titleLbl.setText(parts.length > 0 ? parts[0].trim() : clean);
                    artistLbl.setText(parts.length > 1 ? parts[1].trim() : "");

                    if (isSelected()) {
                        titleLbl.setStyle("-fx-text-fill: " + STAR_WHITE + ";");
                        artistLbl.setStyle("-fx-text-fill: " + PURP_NEON + ";");
                        setStyle(
                            "-fx-background-color: rgba(168,85,247,0.12);" +
                            "-fx-border-color: " + PURP_NEON + " transparent transparent transparent;" +
                            "-fx-border-width: 0 0 0 3;"
                        );
                    } else {
                        titleLbl.setStyle("-fx-text-fill: " + STAR_WHITE + ";");
                        artistLbl.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
                        setStyle(
                            "-fx-background-color: transparent;" +
                            "-fx-border-color: transparent transparent " + DIM + " transparent;" +
                            "-fx-border-width: 0 0 1 0;"
                        );
                    }
                    setGraphic(cellBox);
                }
            }
        });

        // force a repaint when selection changes so the highlighted style applies correctly
        playlistView.getSelectionModel().selectedIndexProperty().addListener(
            (obs, o, n) -> playlistView.refresh()
        );

        HBox playerControls = buildPlayerControls();

        VBox panel = new VBox(16, sectionTitle, playlistView, playerControls);
        panel.setPadding(new Insets(24, 16, 16, 16));
        // Semi-transparent dark card with a subtle purple border
        panel.setStyle(
            "-fx-background-color: rgba(10,4,32,0.82);" +
            "-fx-border-color: rgba(168,85,247,0.2);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;" +
            "-fx-background-radius: 16;"
        );
        DropShadow panelGlow = new DropShadow();
        panelGlow.setColor(Color.web(PURP_NEON, 0.15));
        panelGlow.setRadius(24);
        panel.setEffect(panelGlow);

        VBox wrapper = new VBox(panel);
        wrapper.setPadding(new Insets(16, 0, 16, 0));
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        VBox.setVgrow(panel, Priority.ALWAYS);
        return wrapper;
    }

    // Creates the PLAY, PAUSE, SKIP buttons. All start disabled — enabled after a playlist loads.
    private HBox buildPlayerControls() {
        // PLAY is wider than the others to indicate it's the primary action
        playButton = new Button("PLAY");
        playButton.setPrefWidth(160);
        playButton.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        stylePrimaryBtn(playButton, false);
        playButton.setOnMouseEntered(e -> { if (!playButton.isDisabled()) stylePrimaryBtn(playButton, true);  });
        playButton.setOnMouseExited(e  -> { if (!playButton.isDisabled()) stylePrimaryBtn(playButton, false); });

        pauseButton = createColorButton("PAUSE", PINK_SOFT);
        skipButton  = createColorButton("SKIP",  GOLD);

        // disabled until there's actually a playlist to play
        playButton.setDisable(true);
        pauseButton.setDisable(true);
        skipButton.setDisable(true);

        HBox row = new HBox(8, playButton, pauseButton, skipButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 0, 0));
        return row;
    }

    // Wires all button actions in one place so the build methods stay clean.
    private void connectEventHandlers() {
        generateButton.setOnAction(e -> handleGeneratePlaylist());
        playButton.setOnAction(e     -> handlePlay());
        pauseButton.setOnAction(e    -> handlePause());
        skipButton.setOnAction(e     -> handleSkip());
        // double-clicking a row is a shortcut for pressing PLAY
        playlistView.setOnMouseClicked(e -> {
            Platform.runLater(() -> handlePlay());
        });
    }

    /**
     * Reads the input fields, runs the search, and populates the playlist ListView.
     * Also enables the playback buttons once results are available.
     */
    private void handleGeneratePlaylist() {
        String genre  = genreField.getText().trim();
        String artist = artistField.getText().trim();
        String tags   = tagsField.getText().trim();

        // at least one field needs a value or there's nothing to filter by
        if (genre.isEmpty() && artist.isEmpty() && tags.isEmpty()) {
            statusLabel.setText("Enter at least one preference");
            statusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 10px;");
            return;
        }

        statusLabel.setText("Scanning the cosmos...");
        statusLabel.setStyle("-fx-text-fill: " + GOLD + "; -fx-font-family: Monospace; -fx-font-size: 10px;");
        playlistItems.clear();
        currentPlaylistSongs.clear();

        // split tags on commas or spaces into individual keywords
        List<String> keywords = new ArrayList<>();
        if (!tags.isEmpty()) {
            for (String tag : tags.split("[,\\s]+")) {
                String c = tag.trim();
                if (!c.isEmpty()) keywords.add(c);
            }
        }

        UserPreferences prefs = new UserPreferences(genre, artist, keywords);
        currentPlaylist       = playlistGenerator.generate(prefs);
        List<Song> songs      = currentPlaylist.getSongs();

        if (songs.isEmpty()) {
            statusLabel.setText("No signals detected — try new filters");
            statusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-family: Monospace; -fx-font-size: 10px;");
            return;
        }

        // populate both the display list and the parallel Song list at the same time
        for (int i = 0; i < songs.size(); i++) {
            playlistItems.add((i + 1) + ".   " + songs.get(i).toString());
            currentPlaylistSongs.add(songs.get(i));
        }

        statusLabel.setText(songs.size() + " signal" + (songs.size() == 1 ? "" : "s") + " acquired");
        statusLabel.setStyle("-fx-text-fill: " + CYAN_SOFT + "; -fx-font-family: Monospace; -fx-font-size: 10px;");

        playButton.setDisable(false);
        pauseButton.setDisable(false);
        skipButton.setDisable(false);

        playlistView.getSelectionModel().selectFirst();
        musicPlayer.setPlaylist(currentPlaylist);
    }

    // Plays whatever is selected in the ListView. Defaults to index 0 if nothing is selected.
    private void handlePlay() {
        int idx = playlistView.getSelectionModel().getSelectedIndex();
        if (idx < 0 && !currentPlaylistSongs.isEmpty()) {
            idx = 0;
            playlistView.getSelectionModel().select(0);
        }
        if (idx >= 0 && idx < currentPlaylistSongs.size()) {
            currentSong = currentPlaylistSongs.get(idx);
            musicPlayer.setCurrentSong(currentSong);
            nowPlayingLabel.setText("Now playing � �  " + currentSong.toString());
            nowPlayingLabel.setStyle("-fx-text-fill: " + GOLD + ";");
            DropShadow ng = new DropShadow();
            ng.setColor(Color.web(GOLD, 0.5)); ng.setRadius(10);
            nowPlayingLabel.setEffect(ng);
            musicPlayer.play();
        }
    }

    private void handlePause() {
        nowPlayingLabel.setText("Paused");
        nowPlayingLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
        nowPlayingLabel.setEffect(null); // clear the gold glow
        musicPlayer.pause();
    }

    /**
     * Moves to the next track. MusicPlayer.skip() only updates its internal index —
     * we call play() here after the UI is updated so both stay in sync.
     */
    private void handleSkip() {
        if (currentPlaylistSongs.isEmpty()) return;

        musicPlayer.skip();

        int current = playlistView.getSelectionModel().getSelectedIndex();
        if (current < 0) current = 0;
        int next = (current + 1) % currentPlaylistSongs.size(); // wraps back to 0 at the end
        playlistView.getSelectionModel().select(next);
        currentSong = currentPlaylistSongs.get(next);
        nowPlayingLabel.setText("Now playing  —  " + currentSong.toString());
        nowPlayingLabel.setStyle("-fx-text-fill: " + GOLD + ";");
        DropShadow ng = new DropShadow();
        ng.setColor(Color.web(GOLD, 0.5)); ng.setRadius(10);
        nowPlayingLabel.setEffect(ng);

        musicPlayer.play();
    }

    // Switches the generate button between its idle (outline) and hover (filled) appearances.
    private void styleGenBtn(boolean hover) {
        generateButton.setStyle(
            "-fx-background-color: " + (hover ? PURP_NEON : "transparent") + ";" +
            "-fx-text-fill: " + (hover ? "#ffffff" : PURP_NEON) + ";" +
            "-fx-padding: 12 20 12 20;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + PURP_NEON + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;"
        );
        if (hover) {
            DropShadow g = new DropShadow();
            g.setColor(Color.web(PURP_NEON, 0.5)); g.setRadius(16);
            generateButton.setEffect(g);
        } else {
            generateButton.setEffect(null);
        }
    }

    // Same hover toggle logic as styleGenBtn but for the PLAY button (green colour scheme).
    private void stylePrimaryBtn(Button btn, boolean hover) {
        String GREEN = "#00ffcc";
        btn.setStyle(
            "-fx-background-color: " + (hover ? GREEN : "rgba(0,200,83,0.15)") + ";" +
            "-fx-text-fill: " + (hover ? "#000000" : GREEN) + ";" +
            "-fx-padding: 12 24 12 24;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;" +
            "-fx-border-color: " + GREEN + ";"
        );
        DropShadow g = new DropShadow();
        g.setColor(Color.web(GREEN, hover ? 0.6 : 0.3));
        g.setRadius(hover ? 20 : 10);
        btn.setEffect(g);
    }

    // Factory method used for PAUSE and SKIP — avoids duplicating the hover listener setup.
    private Button createColorButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(110);
        btn.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        applyColorBtnStyle(btn, color, false);
        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) applyColorBtnStyle(btn, color, true);  });
        btn.setOnMouseExited(e  -> { if (!btn.isDisabled()) applyColorBtnStyle(btn, color, false); });
        return btn;
    }

    // Idle: transparent background, coloured text and border.
    // Hover: solid colour fill, dark text, stronger glow.
    private void applyColorBtnStyle(Button btn, String color, boolean hover) {
        btn.setStyle(
            "-fx-background-color: " + (hover ? color : "transparent") + ";" +
            "-fx-text-fill: " + (hover ? DEEP_SPACE : color) + ";" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;" +
            "-fx-border-color: " + color + ";"
        );
        DropShadow g = new DropShadow();
        g.setColor(Color.web(color, hover ? 0.5 : 0.2));
        g.setRadius(hover ? 16 : 6);
        btn.setEffect(g);
    }

    // Creates a styled text field. The border colour changes when the field gains focus
    // so the user can tell which field is active.
    private TextField createField(String placeholder) {
        TextField field = new TextField();
        field.setPromptText(placeholder);
        field.setFont(Font.font("SansSerif", 12));
        applyFieldStyle(field, false);
        field.focusedProperty().addListener((obs, w, focused) -> applyFieldStyle(field, focused));
        return field;
    }

    // Cyan border + glow when focused, dim border when not.
    private void applyFieldStyle(TextField field, boolean focused) {
        field.setStyle(
            "-fx-background-color: rgba(5,0,20,0.7);" +
            "-fx-text-fill: " + STAR_WHITE + ";" +
            "-fx-prompt-text-fill: " + TEXT_MUTED + ";" +
            "-fx-padding: 10 14 10 14;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            (focused
                ? "-fx-border-color: " + CYAN_SOFT + "; -fx-border-width: 1;"
                : "-fx-border-color: " + DIM + "; -fx-border-width: 1;")
        );
        if (focused) {
            DropShadow g = new DropShadow();
            g.setColor(Color.web(CYAN_SOFT, 0.3)); g.setRadius(10);
            field.setEffect(g);
        } else {
            field.setEffect(null);
        }
    }

    // Pairs a small all-caps label with a text field — used for all three input sections.
    private VBox fieldGroup(String caption, TextField field) {
        Label lbl = new Label(caption);
        lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        lbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-letter-spacing: 2px;");
        return new VBox(6, lbl, field);
    }
}