import java.io.IOException;
import java.util.List;

import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;

public class Main {
	public static void main(String[] args) throws HttpStatusException, IOException {
		Scraper scraper = new Scraper();
		scraper.scrapeData();
	}
}
