import java.util.ArrayList;
import java.util.List;

public class PlaylistGenerator {
	private List<Song> database;
	
	public PlaylistGenerator(List<Song> database) {
		this.database = database;
	}
	
	public Playlist generate(UserPreferences prefs) {
		List<Song> result = new ArrayList<>(database);
		
		if(prefs.getGenre() != null && !prefs.getGenre().isEmpty()) {
			result = filterByGenre(result, prefs.getGenre());
		}
		
		if(prefs.getArtist() != null && !prefs.getArtist().isEmpty()) {
			result = filterByArtist(result, prefs.getArtist());
		}
		
		if(prefs.getKeywords() != null && !prefs.getKeywords().isEmpty()) {
			result = filterByTags(result, prefs.getKeywords());
		}
		
		return new Playlist(result, "My Playlist");
	}
	
	private List<Song> filterByGenre(List<Song> songs, String genre){
		List<Song> filtered = new ArrayList<>();
		
		for(Song song : songs) {
			if(song.getGenre().equalsIgnoreCase(genre)) {
				filtered.add(song);
			}
		}
		return filtered;
	}
	
	private List<Song> filterByArtist(List<Song> songs, String artist){
		List<Song> filtered = new ArrayList<>();
		
		for(Song song : songs) {
			if(song.getArtist().equalsIgnoreCase(artist)) {
				filtered.add(song);
			}
		}
		return filtered;
	}
	
	private List<Song> filterByTags(List<Song> songs, List<String> tags){
		List<Song> filtered = new ArrayList<>();
		
		for(Song song : songs) {
			for(String keyword : tags) {
				for(String songTag : song.getTags()) {
					if(songTag.equalsIgnoreCase(keyword)) {
						filtered.add(song);
						break;
					}
				}
				if(filtered.contains(song)) {
					break;
				}
			}
		}
		return filtered;
	}
}
