# Song-playlist-generator

## Requirements
- JDK 21 (Tested with JDK 21.0.10)
- JavaFX SDK 21.0.11

## Team Information
Team 8

## Project Description
Song playlist generator is a Java application that creates a playlist based on the user's request. The request can contain various keywords. The application provides a graphical user interfac built with JavaFX and supports audio playback using WAV files.

## Features
- Generate playlists based on user preferences
- Filter songs by genre
- Filter songs by artist
- Play, pause, stop, and skip songs
- Load information from a JSON database
- User-friendly JavaFX graphical interface

## Running the Application
- Open the project in Eclipse
- Ensure that JDK 21.0.10 is selected as project's JRE
- Add JavaFX libraries to the Modulepath
- Use the following VM arguments:
  `--module-add "PATH_TO_JAVAFX/lib" --add-modules javafx.controls,javafxfxml`
- Run the application
