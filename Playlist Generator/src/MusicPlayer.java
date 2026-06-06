import java.io.File;
import javax.sound.sampled.*;

public class MusicPlayer {
	private Song currentSong;
	private Playlist currentPlaylist;
	private boolean isPlaying;
	private int currentIndex;
	
	private Clip clip;
	private Thread playThread;
	
	public MusicPlayer() {
		currentSong = null;
		currentPlaylist = null;
		isPlaying = false;
		currentIndex = 0;
	}
	
	public void setPlaylist(Playlist playlist) {
		stop();
		
		currentPlaylist = playlist;
		currentIndex = 0;
		
		if(playlist != null && !playlist.getSongs().isEmpty()) {
			currentSong = playlist.getSongs().get(0);
		}
	}
	
	public void play() {
		if(currentSong == null) {
			return;
		}
		
		playThread = new Thread(() -> {
			try {
				if(clip == null) {
					File audioFile = new File(currentSong.getFilePath());
					AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
					
					clip = AudioSystem.getClip();
					clip.open(audioStream);
				}
				clip.start();
				isPlaying = true;
			}catch(Exception e) {
				e.printStackTrace();
			}
		});
		playThread.start();
	}
	
	public void pause() {
		if(clip != null && clip.isRunning()) {
			clip.stop();
		}
		isPlaying = false;
	}
	
	public void stop() {
		if(clip != null) {
			clip.stop();
			clip.close();
			clip = null;
		}
		isPlaying = false;
	}
	
	public void skip() {
		if(currentPlaylist == null || currentPlaylist.getSongs().isEmpty()) {
			return;
		}
		
		stop();
		
		currentIndex++;
		
		if(currentIndex >= currentPlaylist.getSongs().size()) {
			currentIndex = 0;
		}
		
		currentSong = currentPlaylist.getSongs().get(currentIndex);
		
		play();
	}
	
	public Song getCurrentSong() {
		return currentSong;
	}
	
	public String getCurrentSongTitle() {
		if(currentSong == null) {
			return "";
		}
		
		return currentSong.getTitle();
	}
	
	public String getCurrentSongArtist(){
		if(currentSong == null) {
			return "";
		}
		
		return currentSong.getArtist();
	}
	
	public boolean isPlaying() {
		return isPlaying;
	}
}
