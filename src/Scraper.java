import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Collections;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Scraper {
	private static final List<String> userAgent;
	
	static
    {
    	List<String> tmpList = new ArrayList<String>();
    	try {
			Scanner scanner = new Scanner(new File("user-agents.txt"));
			while (scanner.hasNext()){
			    tmpList.add(scanner.next());
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	finally {
    		userAgent = tmpList;
    	}
    }
	
	static Document connectWeb(String url){
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Document document = null;
    	
    	for(int tryTime = 1; tryTime <= 20; ++tryTime) {
    		try {	
        		Random random = new Random();
            	String randomUA = userAgent.get(random.nextInt(userAgent.size()));

            	Jsoup.newSession();
    			
            	document = Jsoup.connect(url)
                        .userAgent(randomUA)
                        .referrer("http://www.google.com")
                        .timeout(120000)
                        .get();
        	} catch (Exception e) {
        		System.out.println("Error connecting to URL: " + e.getMessage() + "(" + url + ")");
        		System.out.println("Tried " + tryTime + " times. Attempting to try again.");
        		
        		try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
        	}
    		if(document != null) break;
    	}
    	return document;
	}
	
	public List<GameScoreData> getGameData(Document document) {
		List<GameScoreData> resList = new ArrayList<GameScoreData>();
		
		// Single Value
		String name = document.getElementsByClass("c-productHero_title g-inner-spacing-bottom-medium g-outer-spacing-top-medium").text();
		String genre = document.getElementsByClass("c-genreList_item")
				               .select(".c-globalButton")
				               .select("span.c-globalButton_label")
				               .first()
				               .text();
		float userScore = 0;
		
		try {
			userScore = Float.parseFloat(document.getElementsByClass("c-productScoreInfo_scoreNumber u-float-right")
					                             .select(".c-siteReviewScore_background-user")
			                                     .select(".c-siteReviewScore")
			                                     .select("span[data-v-4cdca868]").text());
		} catch(Exception e) {
			System.out.println("Skip " + name);
			return resList;
		}
		
		// Multi-value
		List<String> platforms = new ArrayList<String>();
		List<Integer> metaScores = new ArrayList<Integer>();
		
		Elements platformContainer = document.getElementsByClass("c-gamePlatformsSection_list")
											 .select(".c-gamePlatformTile");
		for(Element platformScoreBox : platformContainer) {
			String platform = null;
			if(platformScoreBox.select(".g-text-medium").size() != 0) {
				platform = platformScoreBox.select(".g-text-medium").text();
			}
			else {
				platform = platformScoreBox.select(".c-gamePlatformLogo_icon").select("title").text();
			}
			
			try {
				metaScores.add(Integer.parseInt(platformScoreBox.select(".c-siteReviewScore")
						                       .select("span[data-v-4cdca868]")
						                       .text()));
				platforms.add(platform);
			} catch(Exception e) {
				System.out.println("Skip 1 platform of " + name);
			}
		}
		
		for(int i = 0; i < platforms.size(); ++i) {
			resList.add(new GameScoreData(name, platforms.get(i), genre, metaScores.get(i), userScore));
		}
		
		System.out.println("Collected " + name);
		
		return resList;
	}
	
	private List<String> getLinksInPage(Document document) {
		List<String> res = new ArrayList<String>();
		
		Elements container = document.getElementsByClass("c-productListings_grid g-grid-container u-grid-columns g-inner-spacing-bottom-large");
		
		Elements links = container.select("a[href]");
		for (Element link : links) {
			res.add(link.attr("abs:href"));
		}
		
		container = document.getElementsByClass("c-productListings_grid g-grid-container u-grid-columns g-inner-spacing-bottom-large g-inner-spacing-top-large");
		
		links = container.select("a[href]");
		for (Element link : links) {
			res.add(link.attr("abs:href"));
		}
		
		return res;
	}
	
	private List<String> getAllLinks() {
		List<String> res = new ArrayList<String>();
		
		String basicLink = "https://www.metacritic.com/browse/game/?releaseYearMin=1958&releaseYearMax=2024&page=";
		
		// Get all page links
		List<String> pageLinks = new ArrayList<String>();
		int totalPages = 551;
		for(int i = 1; i <= totalPages; ++i) {
			String pageLink = basicLink + i;
			pageLinks.add(pageLink);
		}
		Collections.shuffle(pageLinks);
		
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	    
    	for (String pageLink : pageLinks) {
    		executor.submit(() -> {
    			Document document = Scraper.connectWeb(pageLink);
                res.addAll(getLinksInPage(document));
                System.out.println(pageLink + " is colected successfully");
            });
    	}
    	
    	executor.shutdown();
    	while (!executor.isTerminated()) {
            // Waiting...
        }
		
		return res;
	}
	
	public void scrapeData() {
		List<String> allLinks = getAllLinks();
		
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<GameScoreData> dataList = new ArrayList<GameScoreData>();
    	for (String link : allLinks) {
    		executor.submit(() -> {
    			Document document = Scraper.connectWeb(link);
                dataList.addAll(getGameData(document));
            });
    	}
    	
    	executor.shutdown();
    	while (!executor.isTerminated()) {
            // Waiting...
        }
		
		Scraper.exportToCSV(dataList, "game_scores.csv");
	}
	
	public static void exportToCSV(List<GameScoreData> dataList, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header
            writer.append("Name,Platform,Genre,MetaScore,UserScore\n");
            
            // Write data
            for (GameScoreData data : dataList) {
                writer.append(String.join(",", 
                        data.getName(), 
                        data.getPlatform(), 
                        data.getGenre(), 
                        String.valueOf(data.getMetaScore()), 
                        String.valueOf(data.getUserScore())))
                      .append("\n");
            }
            
            writer.flush();
            System.out.println("CSV file exported successfully !!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
