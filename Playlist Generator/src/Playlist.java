import java.util.List;


public class Playlist {
	private List<Song> songs;
	private String name;
	
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
}
