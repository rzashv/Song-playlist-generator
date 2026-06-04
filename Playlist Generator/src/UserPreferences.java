import java.util.List;


public class UserPreferences {
	private String genre;
	private String artist;
	private List<String> keywords;
	
	public UserPreferences(String genre, String artist, List<String> keywords) {
		this.genre = genre;
		this.artist = artist;
		this.keywords = keywords;
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
