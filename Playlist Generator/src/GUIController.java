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
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GUIController — Main JavaFX Application class for the Music Playlist Generator.
 *
 * This class is responsible for:
 *   - Building and managing the entire user interface (header, input panel, playlist panel, footer)
 *   - Handling all user interactions: generate playlist, play, pause, and skip tracks
 *   - Delegating all business logic to PlaylistGenerator and MusicPlayer (separation of concerns)
 *   - Animating an interactive starfield background using JavaFX Timeline
 *
 * Advanced Technology Used : JavaFX GUI (Lab 11 — GUI)
 * Creative Feature          : Animated starfield canvas rendered via JavaFX Timeline
 * Data Source               : JSON-based song database loaded at runtime (Lab 7 — File I/O)
 *
 * Course Topics Applied:
 *   Lab 4  — Classes and Objects   : Song, Playlist, UserPreferences, MusicPlayer
 *   Lab 7  — File I/O              : SongDatabaseLoader reads songs.json from disk
 *   Lab 8  — Error Handling        : Input validation and null-safe status messages
 *   Lab 10 — Generic Programming   : List<Song>, ObservableList<String> throughout
 *   Lab 11 — GUI                   : Full JavaFX interface with controls and animations
 */
public class GUIController extends Application {

    // ── Input fields for the user to specify their music preferences ──────────
    private TextField genreField;
    private TextField artistField;
    private TextField tagsField;

    // ── Playback control buttons ───────────────────────────────────────────────
    private Button generateButton;
    private Button playButton;
    private Button pauseButton;
    private Button skipButton;

    // ── Playlist display: observable list keeps the ListView in sync automatically ──
    private ListView<String> playlistView;
    private ObservableList<String> playlistItems;
    private List<Song> currentPlaylistSongs = new ArrayList<>();

    // ── Status / now-playing labels shown in the footer and input panel ───────
    private Label nowPlayingLabel;
    private Label statusLabel;

    // ── Core backend objects (business logic lives here, not in this class) ───
    private PlaylistGenerator playlistGenerator;
    private MusicPlayer musicPlayer;
    private List<Song> allSongs;       // full song database loaded from JSON
    private Song currentSong;          // the track currently selected/playing
    private Playlist currentPlaylist;  // the last generated playlist

    // ── Colour palette constants (hex strings used throughout inline CSS) ─────
    private static final String DEEP_SPACE  = "#050012"; // near-black background
    private static final String STAR_WHITE  = "#ece6ff"; // primary text colour
    private static final String PURP_NEON   = "#a855f7"; // purple accent (playlist panel)
    private static final String CYAN_SOFT   = "#22d3ee"; // cyan accent (input panel)
    private static final String PINK_SOFT   = "#f472b6"; // pink accent (pause button)
    private static final String GOLD        = "#fbbf24"; // gold accent (skip / now-playing)
    private static final String DIM         = "#2e1a4a"; // subtle border / divider colour
    private static final String TEXT_MUTED  = "#6b4f8a"; // secondary / placeholder text

    // ── Starfield animation data ───────────────────────────────────────────────
    // Parallel arrays store position, size, and opacity for each star
    private double[] starX, starY, starSize, starOpacity;
    private Timeline starTwinkle; // JavaFX Timeline drives the twinkling animation loop
    private Random rnd = new Random();

    /**
     * JavaFX entry point — called automatically when the application launches.
     * Sets up the backend, builds the scene graph, and shows the primary stage.
     *
     * @param stage the primary window provided by the JavaFX runtime
     */
    @Override
    public void start(Stage stage) {
        initBackend();
        initStars(940, 680);

        // StackPane lets the starfield canvas sit behind the UI overlay
        StackPane root = new StackPane();
        Canvas starCanvas = new Canvas(940, 680);
        drawStarfield(starCanvas.getGraphicsContext2D(), 940, 680);

        // Timeline fires every 100 ms to update star opacity (twinkling effect)
        starTwinkle = new Timeline(new KeyFrame(Duration.millis(100), e ->
            twinkleStars(starCanvas.getGraphicsContext2D(), 940, 680)
        ));
        starTwinkle.setCycleCount(Animation.INDEFINITE); // loop forever
        starTwinkle.play();

        VBox ui = buildUI();
        root.getChildren().addAll(starCanvas, ui); // canvas first = rendered behind UI

        Scene scene = new Scene(root, 940, 680);
        scene.setFill(Color.web(DEEP_SPACE));
        stage.setTitle("Music Playlist Generator");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(580);
        stage.show();
    }

    /**
     * Initialises backend dependencies: loads the song database from JSON,
     * creates the PlaylistGenerator, and instantiates the MusicPlayer.
     * Demonstrates File I/O (Lab 7) and class separation (Lab 4).
     */
    private void initBackend() {
        try {
            SongDatabaseLoader loader = new SongDatabaseLoader();
            // Load songs.json as a classpath resource so the path works on any machine
            String path = getClass().getResource("/songs.json").toExternalForm().replace("file:", "");
            allSongs = loader.loadSongs(path);
            playlistGenerator = new PlaylistGenerator(allSongs);
            musicPlayer = new MusicPlayer();
        } catch (Exception e) {
            // If the database fails to load, start with an empty list instead of crashing
            allSongs = new ArrayList<>();
            System.err.println("Failed to load song database: " + e.getMessage());
        }
    }

    /**
     * Populates the parallel starfield arrays with random positions, sizes,
     * and opacities for {@code count} stars spread across the given canvas area.
     *
     * @param w canvas width in pixels
     * @param h canvas height in pixels
     */
    private void initStars(double w, double h) {
        int count = 160; // total number of background stars
        starX = new double[count]; starY = new double[count];
        starSize = new double[count]; starOpacity = new double[count];
        for (int i = 0; i < count; i++) {
            starX[i] = rnd.nextDouble() * w;
            starY[i] = rnd.nextDouble() * h;
            starSize[i] = 0.5 + rnd.nextDouble() * 1.8;   // radius between 0.5 and 2.3 px
            starOpacity[i] = 0.2 + rnd.nextDouble() * 0.6; // opacity between 0.2 and 0.8
        }
    }

    /**
     * Draws the full starfield background: deep-space base colour, three soft
     * nebula ovals for depth, then all stars via {@link #drawStars(GraphicsContext)}.
     *
     * @param gc the GraphicsContext of the background canvas
     * @param w  canvas width
     * @param h  canvas height
     */
    private void drawStarfield(GraphicsContext gc, double w, double h) {
        // Fill base background
        gc.setFill(Color.web(DEEP_SPACE));
        gc.fillRect(0, 0, w, h);
        // Layered nebula ovals — semi-transparent to add depth without obscuring stars
        gc.setFill(Color.web("#1a0840", 0.5));
        gc.fillOval(w * 0.25, h * 0.1, w * 0.55, h * 0.8);
        gc.setFill(Color.web("#0a1030", 0.3));
        gc.fillOval(0, h * 0.3, w * 0.4, h * 0.5);
        gc.setFill(Color.web("#200830", 0.2));
        gc.fillOval(w * 0.6, 0, w * 0.4, h * 0.5);
        drawStars(gc);
    }

    /**
     * Renders each star as a small filled oval using its current opacity value.
     *
     * @param gc the GraphicsContext to draw onto
     */
    private void drawStars(GraphicsContext gc) {
        for (int i = 0; i < starX.length; i++) {
            gc.setFill(Color.web(STAR_WHITE, starOpacity[i]));
            gc.fillOval(starX[i], starY[i], starSize[i], starSize[i]);
        }
    }

    /**
     * Called every 100 ms by {@link #starTwinkle} to animate the starfield.
     * Redraws the background, then randomly varies the opacity of 8 stars
     * per frame to produce a natural twinkling effect.
     *
     * @param gc the GraphicsContext of the background canvas
     * @param w  canvas width
     * @param h  canvas height
     */
    private void twinkleStars(GraphicsContext gc, double w, double h) {
        drawStarfield(gc, w, h);
        // Randomly vary opacity of 8 stars per frame to simulate twinkling
        for (int i = 0; i < 8; i++) {
            int idx = rnd.nextInt(starX.length);
            starOpacity[idx] = 0.15 + rnd.nextDouble() * 0.75;
        }
        drawStars(gc);
    }

    /**
     * Assembles the top-level UI layout: header, main content area (input +
     * playlist panels side by side), and footer.
     *
     * @return the root VBox that is layered on top of the starfield canvas
     */
    private VBox buildUI() {
        VBox inputPanel    = buildInputPanel();
        VBox playlistPanel = buildPlaylistPanel();

        // HBox holds the two panels side by side; playlist panel grows to fill space
        HBox mainContent = new HBox(16, inputPanel, playlistPanel);
        mainContent.setPadding(new Insets(0, 16, 0, 16));
        HBox.setHgrow(playlistPanel, Priority.ALWAYS);

        VBox header = buildHeader();
        HBox footer = buildFooter();

        VBox root = new VBox(0, header, mainContent, footer);
        root.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(mainContent, Priority.ALWAYS); // main area takes remaining height

        connectEventHandlers();
        return root;
    }

    /**
     * Builds the application header containing the title label (with glow effect),
     * a subtitle showing the total track count, and a decorative gradient divider.
     *
     * @return a VBox containing all header elements
     */
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

        // Purple neon glow effect applied to the title for visual emphasis
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(PURP_NEON, 0.7));
        glow.setRadius(22); glow.setSpread(0.2);
        title.setEffect(glow);

        HBox titleRow = new HBox(12, starLeft, title, starRight);
        titleRow.setAlignment(Pos.CENTER);

        // Display total number of tracks loaded from the JSON database
        int count = (allSongs != null) ? allSongs.size() : 0;
        Label sub = new Label(count + " tracks in the universe");
        sub.setFont(Font.font("Monospace", 10));
        sub.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-letter-spacing: 2px;");

        VBox center = new VBox(6, titleRow, sub);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(20, 24, 16, 24));

        // Thin gradient line separating the header from the main content
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

    /**
     * Builds the footer bar containing the "now playing" label on the left
     * and the group/year tagline on the right.
     *
     * @return an HBox wrapping the footer VBox
     */
    private HBox buildFooter() {
        nowPlayingLabel = new Label("No track selected");
        nowPlayingLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
        nowPlayingLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");

        // Spacer pushes the tagline to the far right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tagline = new Label("Group 8  ·  2026");
        tagline.setFont(Font.font("SansSerif", 10));
        tagline.setStyle("-fx-text-fill: " + DIM + ";");

        // Thin top border line for the footer
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
     * Builds the left input panel ("NAVIGATION") containing:
     * <ul>
     *   <li>Genre, Artist, and Tags text fields</li>
     *   <li>The "LAUNCH SEARCH" generate button</li>
     *   <li>A status label showing database state or search results</li>
     * </ul>
     *
     * @return a VBox wrapper containing the styled input panel
     */
    private VBox buildInputPanel() {
        Label sectionTitle = new Label("NAVIGATION");
        sectionTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        sectionTitle.setStyle("-fx-text-fill: " + CYAN_SOFT + "; -fx-letter-spacing: 3px;");
        DropShadow sg = new DropShadow();
        sg.setColor(Color.web(CYAN_SOFT, 0.4)); sg.setRadius(8);
        sectionTitle.setEffect(sg);

        // Text fields for user preference input
        genreField  = createField("e.g.  Pop,  Jazz,  Rock");
        artistField = createField("e.g.  Adele,  Eminem");
        tagsField   = createField("e.g.  calm,  workout");

        // Wrap each field with a labelled caption group
        VBox genreBox  = fieldGroup("GENRE",  genreField);
        VBox artistBox = fieldGroup("ARTIST", artistField);
        VBox tagsBox   = fieldGroup("TAGS",   tagsField);

        generateButton = new Button("LAUNCH SEARCH");
        generateButton.setMaxWidth(Double.MAX_VALUE);
        generateButton.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        styleGenBtn(false);
        // Hover listeners toggle the button fill between outline and solid purple
        generateButton.setOnMouseEntered(e -> styleGenBtn(true));
        generateButton.setOnMouseExited(e  -> styleGenBtn(false));

        // Status label — shows database load result or search outcome
        statusLabel = new Label(allSongs != null && !allSongs.isEmpty()
            ? allSongs.size() + " signals ready"
            : "Error: database not found");
        statusLabel.setFont(Font.font("Monospace", 10));
        statusLabel.setStyle(
            (allSongs != null && !allSongs.isEmpty())
                ? "-fx-text-fill: " + CYAN_SOFT + ";"
                : "-fx-text-fill: #f87171;" // red tint indicates an error state
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
     * Builds the right playlist panel ("DISCOVERED TRACKS") containing:
     * <ul>
     *   <li>A custom-styled ListView with two-line cells (title + artist)</li>
     *   <li>Player control buttons (PLAY, PAUSE, SKIP) at the bottom</li>
     * </ul>
     * The ListView uses a custom CellFactory to split the track string into
     * separate title and artist labels with distinct styling.
     *
     * @return a VBox wrapper containing the styled playlist panel
     */
    private VBox buildPlaylistPanel() {
        Label sectionTitle = new Label("DISCOVERED TRACKS");
        sectionTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        sectionTitle.setStyle("-fx-text-fill: " + PURP_NEON + "; -fx-letter-spacing: 3px;");
        DropShadow cg = new DropShadow();
        cg.setColor(Color.web(PURP_NEON, 0.4)); cg.setRadius(8);
        sectionTitle.setEffect(cg);

        // ObservableList automatically updates the ListView when items are added/removed
        playlistItems = FXCollections.observableArrayList();
        playlistView  = new ListView<>(playlistItems);
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        playlistView.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background-insets: 0;" +
            "-fx-border-width: 0;" +
            "-fx-padding: 4;"
        );

        // Shown when no tracks have been generated yet
        Label placeholder = new Label("Chart a course and launch your search");
        placeholder.setFont(Font.font("SansSerif", 12));
        placeholder.setStyle("-fx-text-fill: " + DIM + ";");
        playlistView.setPlaceholder(placeholder);

        // Custom CellFactory: splits "Title - Artist" string into two styled labels
        playlistView.setCellFactory(lv -> new ListCell<String>() {
            private final Label titleLbl  = new Label();
            private final Label artistLbl = new Label();
            private final VBox  cellBox   = new VBox(2, titleLbl, artistLbl);
            {
                // Instance initializer block — runs once per cell at construction time
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
                    // Strip the leading numbering ("1.   ") before splitting
                    String clean  = item.replaceFirst("^\\d+\\.\\s*", "");
                    String[] parts = clean.split(" - ", 2);
                    titleLbl.setText(parts.length > 0 ? parts[0].trim() : clean);
                    artistLbl.setText(parts.length > 1 ? parts[1].trim() : "");

                    // Selected row: bright text + purple left-border highlight
                    if (isSelected()) {
                        titleLbl.setStyle("-fx-text-fill: " + STAR_WHITE + ";");
                        artistLbl.setStyle("-fx-text-fill: " + PURP_NEON + ";");
                        setStyle(
                            "-fx-background-color: rgba(168,85,247,0.12);" +
                            "-fx-border-color: " + PURP_NEON + " transparent transparent transparent;" +
                            "-fx-border-width: 0 0 0 3;"
                        );
                    } else {
                        // Unselected row: muted artist text + subtle bottom divider
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

        // Refresh the list when selection changes so cell styles update correctly
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

    /**
     * Builds the row of playback control buttons:
     * PLAY (green), PAUSE (pink), SKIP (gold).
     * All three buttons are initially disabled until a playlist is generated.
     *
     * @return an HBox containing the three control buttons
     */
    private HBox buildPlayerControls() {
        // PLAY — green, wider button to indicate primary action
        playButton = new Button("PLAY");
        playButton.setPrefWidth(160);
        playButton.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        stylePrimaryBtn(playButton, false);
        // Hover effect: fill transitions from transparent to solid green
        playButton.setOnMouseEntered(e -> { if (!playButton.isDisabled()) stylePrimaryBtn(playButton, true);  });
        playButton.setOnMouseExited(e  -> { if (!playButton.isDisabled()) stylePrimaryBtn(playButton, false); });

        // PAUSE — pink
        pauseButton = createColorButton("PAUSE", PINK_SOFT);

        // SKIP — gold
        skipButton  = createColorButton("SKIP",  GOLD);

        // Disable all controls until a playlist has been generated
        playButton.setDisable(true);
        pauseButton.setDisable(true);
        skipButton.setDisable(true);

        HBox row = new HBox(8, playButton, pauseButton, skipButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 0, 0));
        return row;
    }

    /**
     * Wires all event handlers to their corresponding buttons and the ListView.
     * Double-clicking a track in the list triggers playback immediately.
     */
    private void connectEventHandlers() {
        generateButton.setOnAction(e -> handleGeneratePlaylist());
        playButton.setOnAction(e     -> handlePlay());
        pauseButton.setOnAction(e    -> handlePause());
        skipButton.setOnAction(e     -> handleSkip());
        // Double-click on a list row acts as a shortcut for the PLAY button
        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) handlePlay();
        });
    }

    /**
     * Handles the "LAUNCH SEARCH" button action.
     * Validates that at least one preference field is filled, builds a
     * {@link UserPreferences} object, delegates to {@link PlaylistGenerator},
     * and populates the ListView with the resulting tracks.
     *
     * Demonstrates: Error Handling (Lab 8), Generic Programming (Lab 10),
     *               OOP delegation (Lab 4).
     */
    private void handleGeneratePlaylist() {
        String genre  = genreField.getText().trim();
        String artist = artistField.getText().trim();
        String tags   = tagsField.getText().trim();

        // Validate: at least one field must be non-empty before searching
        if (genre.isEmpty() && artist.isEmpty() && tags.isEmpty()) {
            statusLabel.setText("Enter at least one preference");
            statusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 10px;");
            return;
        }

        statusLabel.setText("Scanning the cosmos...");
        statusLabel.setStyle("-fx-text-fill: " + GOLD + "; -fx-font-family: Monospace; -fx-font-size: 10px;");
        playlistItems.clear();
        currentPlaylistSongs.clear();

        // Parse comma/space-separated tags into a keyword list
        List<String> keywords = new ArrayList<>();
        if (!tags.isEmpty()) {
            for (String tag : tags.split("[,\\s]+")) {
                String c = tag.trim();
                if (!c.isEmpty()) keywords.add(c);
            }
        }

        // Build preferences and delegate playlist generation to PlaylistGenerator
        UserPreferences prefs = new UserPreferences(genre, artist, keywords);
        currentPlaylist       = playlistGenerator.generate(prefs);
        List<Song> songs      = currentPlaylist.getSongs();

        // Handle the case where no matching songs are found
        if (songs.isEmpty()) {
            statusLabel.setText("No signals detected — try new filters");
            statusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-family: Monospace; -fx-font-size: 10px;");
            return;
        }

        // Populate the ListView and the parallel song list with the results
        for (int i = 0; i < songs.size(); i++) {
            playlistItems.add((i + 1) + ".   " + songs.get(i).toString());
            currentPlaylistSongs.add(songs.get(i));
        }

        // Pluralise "signal" correctly for the status message
        statusLabel.setText(songs.size() + " signal" + (songs.size() == 1 ? "" : "s") + " acquired");
        statusLabel.setStyle("-fx-text-fill: " + CYAN_SOFT + "; -fx-font-family: Monospace; -fx-font-size: 10px;");

        // Enable playback controls now that a playlist exists
        playButton.setDisable(false);
        pauseButton.setDisable(false);
        skipButton.setDisable(false);

        playlistView.getSelectionModel().selectFirst();
        musicPlayer.setPlaylist(currentPlaylist);
    }

    /**
     * Handles the PLAY button (and double-click on a list row).
     * Resolves the selected track, updates the "now playing" footer label,
     * and delegates actual playback to {@link MusicPlayer}.
     */
    private void handlePlay() {
        int idx = playlistView.getSelectionModel().getSelectedIndex();
        // Default to the first track if nothing is selected
        if (idx < 0 && !currentPlaylistSongs.isEmpty()) {
            idx = 0;
            playlistView.getSelectionModel().select(0);
        }
        if (idx >= 0 && idx < currentPlaylistSongs.size()) {
            currentSong = currentPlaylistSongs.get(idx);
            nowPlayingLabel.setText("Now playing  —  " + currentSong.toString());
            nowPlayingLabel.setStyle("-fx-text-fill: " + GOLD + ";");
            // Gold glow effect on the now-playing label for visual feedback
            DropShadow ng = new DropShadow();
            ng.setColor(Color.web(GOLD, 0.5)); ng.setRadius(10);
            nowPlayingLabel.setEffect(ng);
            musicPlayer.play();
        }
    }

    /**
     * Handles the PAUSE button.
     * Resets the footer label to a neutral "Paused" state and
     * delegates the pause action to {@link MusicPlayer}.
     */
    private void handlePause() {
        nowPlayingLabel.setText("Paused");
        nowPlayingLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
        nowPlayingLabel.setEffect(null); // remove the gold glow
        musicPlayer.pause();
    }

    /**
     * Handles the SKIP button.
     * Advances to the next track in the playlist (wraps around to the first
     * track if at the end), updates the ListView selection, and refreshes
     * the footer "now playing" label.
     */
    private void handleSkip() {
        // Guard against empty playlist to prevent IndexOutOfBoundsException
        if (currentPlaylistSongs.isEmpty()) return;
        
        // Update MusicPlayer's internal index and stop current audio,
        // but do not trigger play() inside MusicPlayer — we control that here
        musicPlayer.skip();
        
        int current = playlistView.getSelectionModel().getSelectedIndex();
        if (current < 0) current = 0;
        // Wrap-around: after the last track, go back to the first
        int next = (current + 1) % currentPlaylistSongs.size();
        playlistView.getSelectionModel().select(next);
        currentSong = currentPlaylistSongs.get(next);
        nowPlayingLabel.setText("Now playing  —  " + currentSong.toString());
        nowPlayingLabel.setStyle("-fx-text-fill: " + GOLD + ";");
        DropShadow ng = new DropShadow();
        ng.setColor(Color.web(GOLD, 0.5)); ng.setRadius(10);
        nowPlayingLabel.setEffect(ng);
        
        // Now trigger playback after the UI and MusicPlayer index are in sync
        musicPlayer.play();
    }

    /**
     * Applies the "LAUNCH SEARCH" button style dynamically.
     * On hover: solid purple fill with glow. On idle: transparent with purple border.
     *
     * @param hover {@code true} if the mouse is currently over the button
     */
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

    /**
     * Applies the primary (PLAY) button style dynamically.
     * On hover: solid green fill with strong glow. On idle: transparent green tint.
     *
     * @param btn   the button to style
     * @param hover {@code true} if the mouse is currently over the button
     */
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

    /**
     * Factory method for creating a uniformly styled colour-accent button
     * (used for PAUSE and SKIP). Hover state transitions between outline and
     * filled using the provided accent colour.
     *
     * @param text  the button label
     * @param color hex colour string for the button's accent
     * @return a fully styled Button with hover listeners attached
     */
    private Button createColorButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(110);
        btn.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        applyColorBtnStyle(btn, color, false);
        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) applyColorBtnStyle(btn, color, true);  });
        btn.setOnMouseExited(e  -> { if (!btn.isDisabled()) applyColorBtnStyle(btn, color, false); });
        return btn;
    }

    /**
     * Applies inline CSS to a colour-accent button based on hover state.
     * Idle: transparent background with coloured border and text.
     * Hover: solid colour background with dark text and stronger glow.
     *
     * @param btn   the button to style
     * @param color hex colour string for the button's accent
     * @param hover {@code true} if the mouse is currently over the button
     */
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

    /**
     * Creates a styled TextField with a placeholder prompt and dynamic focus styling.
     * The border colour transitions from dim to cyan when the field gains focus.
     *
     * @param placeholder the prompt text shown when the field is empty
     * @return a configured TextField with focus listeners attached
     */
    private TextField createField(String placeholder) {
        TextField field = new TextField();
        field.setPromptText(placeholder);
        field.setFont(Font.font("SansSerif", 12));
        applyFieldStyle(field, false);
        // Focus listener: re-applies style with cyan border when field is focused
        field.focusedProperty().addListener((obs, w, focused) -> applyFieldStyle(field, focused));
        return field;
    }

    /**
     * Applies inline CSS to a TextField based on focus state.
     * Focused: cyan border with a soft glow. Unfocused: dim border, no glow.
     *
     * @param field   the TextField to style
     * @param focused {@code true} if the field currently has keyboard focus
     */
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

    /**
     * Creates a labelled field group: a small all-caps caption label stacked
     * above the given TextField, used consistently across the input panel.
     *
     * @param caption the uppercase label text (e.g. "GENRE", "ARTIST")
     * @param field   the TextField to place below the caption
     * @return a VBox containing the label and field
     */
    private VBox fieldGroup(String caption, TextField field) {
        Label lbl = new Label(caption);
        lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        lbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-letter-spacing: 2px;");
        return new VBox(6, lbl, field);
    }
}