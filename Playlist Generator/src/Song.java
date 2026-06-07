import java.util.List;
import java.util.ArrayList;

public class Song {
	private String title;
	private String artist;
	private String genre;
	private String album;
	private String filePath;
	private List<String> tags;
	
	public Song() {
		
	}
	
	public Song(String title, String artist, 
			String genre, String album,
			String filePath, List<String> tags) {
		this.title = title;
		this.artist = artist;
		this.genre = genre;
		this.album = album;
		this.filePath = filePath;
		this.tags = tags;
	}
	
	public String getInfo() {
		return "Title: " + title + 
				"\nArtist: " + artist + 
				"\nGenre: " + genre + 
				"\nAlbum: " + album;
	}
	
	public String getTitle() {
		return title;
	}
	public String getArtist() {
		return artist;
	}
	public String getGenre() {
		return genre;
	}
	public String getAlbum() {
		return album;
	}
	public String getFilePath() {
		return filePath;
	}
	public List<String> getTags() {
		if (tags == null) {
			return new ArrayList<>();
		}
		return tags;
	}
	
	@Override
	public String toString() {
		return title + " - " + artist + "  [" + album + "]";
	}
}
