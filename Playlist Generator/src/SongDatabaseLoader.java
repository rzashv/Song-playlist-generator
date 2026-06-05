import java.util.List;
import java.util.ArrayList;
import java.io.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class SongDatabaseLoader {
	public SongDatabaseLoader() {}
	
	public List<Song> loadSongs(String jsonFilePath) {
		List<Song> songList = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		String line;
		
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(
						new FileInputStream(jsonFilePath)))) {
			
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			
			Gson gson = new Gson();
			Song[] songArray = gson.fromJson(sb.toString(), Song[].class);
			
			if (songArray == null) return new ArrayList<>();
			
			for (Song song : songArray) {
				songList.add(song);
			}
			return songList;
			
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + e.getMessage());
			return new ArrayList<>();
		} catch (IOException e) {
			System.out.println("File reading error: " + e.getMessage());
			return new ArrayList<>();
		} catch (JsonSyntaxException e) {
			System.out.println("JSON file parsing error: " + e.getMessage());
			return new ArrayList<>();
		}
	}
	
	
	
	
}
