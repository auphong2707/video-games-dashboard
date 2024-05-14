
public class GameScoreData {
	private String name;
	private String platform;
	private String genre;
	private int metaScore;
	private float userScore;
	

	public GameScoreData(String name, String platform, String genre, int metaScore, float userScore) {
		super();
		this.name = name;
		this.platform = platform;
		this.genre = genre;
		this.metaScore = metaScore;
		this.userScore = userScore;
	}
	
	public String getName() {
		return name;
	}
	public String getPlatform() {
		return platform;
	}
	public String getGenre() {
		return genre;
	}
	public int getMetaScore() {
		return metaScore;
	}
	public float getUserScore() {
		return userScore;
	}
}
