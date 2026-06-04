import java.util.List;


public class UserPreferences {
	private String genre;
	private String artist;
	private List<String> keywords;
	
	public static String getGenre(UserPreferences preference) {
		return preference.genre;
	}
	public static String getArtist(UserPreferences preference) {
		return preference.artist;
	}
	public static List<String> getKeywords(UserPreferences preference) {
		return preference.keywords;
	}
}
