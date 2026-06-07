import java.util.List;
import java.util.ArrayList;


public class UserPreferences {
	private String genre;
	private String artist;
	private List<String> keywords;
	
	public UserPreferences(String genre, String artist, List<String> keywords) {
		this.genre = genre;
		this.artist = artist;
		
		if (keywords == null) {
			this.keywords =new ArrayList<>();
		} else {
			this.keywords = keywords;
		}
	}
	
	public String getGenre() {
		return genre;
	}
	public String getArtist() {
		return artist;
	}
	public List<String> getKeywords() {
		return keywords;
	}
}
