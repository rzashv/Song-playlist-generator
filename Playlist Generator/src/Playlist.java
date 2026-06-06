import java.util.List;
import java.util.ArrayList;


public class Playlist {
	private List<Song> songs;
	private String name;
	
	public Playlist() {
		songs = new ArrayList<>();
		name = "New Playlist";
	}
	
	public Playlist(List<Song> songs, String name) {
		this.songs = songs;
		this.name = name;
	}
	
	public List<Song> getSongs() {
		return songs;
	}
	public String getName() {
		return name;
	}
	
	
	public void addSong(Song newSong) {
		songs.add(newSong);
	}
	
	public void removeSong(Song remSong) {
		songs.remove(remSong);
	}
	
	public int size() {
		return songs.size();  // in case we need to use size of the playlist
	}

}
