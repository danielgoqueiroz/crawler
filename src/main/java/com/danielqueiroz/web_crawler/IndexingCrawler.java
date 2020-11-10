package com.danielqueiroz.web_crawler;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.google.common.io.Files;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;

public class IndexingCrawler extends WebCrawler {

	private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg" + "|png|mp3|mp4|zip|gz))$");

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith("https://ndmais.com.br/");
	}

	/**
	 * This function is called when a page is fetched and ready to be processed by
	 * your program.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();
		System.out.println("URL: " + url);

		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String html = htmlParseData.getHtml();
			Document document = Jsoup.parse(html);

			Elements imgTags = document.getElementsByTag("Img");
			for (Element el : imgTags) {
				String srcText = el.attr("src");
				if (srcText.contains("https://static.ndonline.com.br")) {
					String attr = el.attr("srcset");
					String captionId = el.getAllElements().attr("describedby");
					if (!captionId.isEmpty()) {
						Element capitionEl = document.getElementById(captionId);
						String ownText = capitionEl.ownText();
						if (ownText.contains("Foto:")) {
							String photographerName = ownText.split("Foto:")[1];
							if (!attr.trim().isEmpty()) {
								String[] splitText = attr.split(" ");
								attr = splitText[0];
								try {
								       MessageDigest digest = MessageDigest.getInstance("MD5");

									String md5Text = photographerName + attr;
									FileUtils.writeByteArrayToFile(//
											new File("target/urlsList/urls.txt"),
											(photographerName + "," + attr + "\n").getBytes(),true);
								} catch (IOException | NoSuchAlgorithmException e) {
									e.printStackTrace();
								}
							}
						}

					}

				}
			}

			// Set<WebURL> links = htmlParseData.getOutgoingUrls();

		}
	}

	public static void main(String[] args) throws Exception {
		String crawlStorageFolder = "target/urls";
		int numberOfCrawlers = 7;

		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(crawlStorageFolder);

		// Instantiate the controller for this crawl.
		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

		// For each crawl, you need to add some seed urls. These are the first
		// URLs that are fetched and then the crawler starts following links
		// which are found in these pages
		controller.addSeed("https://www.ndmais.com.br/");
		controller.addSeed("https://www.ndmais.com.br/noticias/");

		CrawlController.WebCrawlerFactory<IndexingCrawler> factory = IndexingCrawler::new;

		// Start the crawl. This is a blocking operation, meaning that your code
		// will reach the line after this only when crawling is finished.
		controller.start(factory, numberOfCrawlers);
	}

}
