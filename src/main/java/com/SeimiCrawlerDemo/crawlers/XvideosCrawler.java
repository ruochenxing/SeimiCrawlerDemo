package com.SeimiCrawlerDemo.crawlers;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.SeimiCrawlerDemo.Contants;
import com.alibaba.fastjson.JSONObject;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.core.Seimi;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "xvideo", useCookie = true, delay = 1)
public class XvideosCrawler extends BaseSeimiCrawler {
	public static final String DOMAIN = "http://www.xvideos.com";
	public static final String DOWNLOAD_URL = "http://www.xvideos.com/video-download/";
	public static final String VIDEODIR = Contants.VIDEODIR;
	private static final String COOKIES = "wpn-popupunder=1; html5_networkspeed=4258; multi_accounts=bc6e2ef95ce90e6aZClhxSL4M92Z3UqeykMvVfWyr4unay1M9x8ABYzl_ikKDqJkKLL-KQcaBSyAGPoz; X-Backend=2|WcZId|WcZId; hexavid_lastsubscheck=1; speedstats_vthumbs=%7B%222%22%3A%5B%7B%22s%22%3A1%2C%22d%22%3A875%7D%2C%7B%22s%22%3A2%2C%22d%22%3A611%7D%5D%2C%223%22%3A%5B%7B%22s%22%3A1%2C%22d%22%3A1585%7D%2C%7B%22s%22%3A0%2C%22d%22%3A515%7D%2C%7B%22s%22%3A1%2C%22d%22%3A623%7D%5D%2C%227%22%3A%5B%7B%22s%22%3A1%2C%22d%22%3A574%7D%2C%7B%22s%22%3A1%2C%22d%22%3A890%7D%2C%7B%22s%22%3A2%2C%22d%22%3A909%7D%5D%2C%22last%22%3A%7B%22s%22%3A1%2C%22v%22%3A%5B623%5D%7D%7D; HEXAVID_LOGIN=19043622914715eaNwf6glXtSYMNTMQMRRF1oXqL2aCLR5ARNlTv8Zbe7oENb3wU69RijqcgoFQ3Xqn8uESRMa3uXQ58XO-7fnTXYNdNJPnWdU2sFFME3pz6Og_tY_3N-h9djmLov0u_idIdrP9Bx0bCSNb_ZBm6kW6NkSijYYjwxM0aa-vRMFMbKJRinF-bjzEf2obTWPdxfVq7h6Mpq-k1xwAFavMs7HdhtBSVbYAiRFXodENRd0GPiSMUtKQ9BfRYPn6K6ULXQULMoPJwAYXuv5qNnkdqesGiw4aWcqPosT7LfmyqRqKfhHFVYSu5VTIZkAee0CnlAN5t0r7HEJhdf8RJ239LHufbrA8Sb5IhXUWjriRbFk_RUbEz3nH0Ylxn6msM_kdIh-xYqm9aZyOXPbg1_lSEsk5SOpsigGHWfTvrO7L5IaXsuv4rN4fPRcUVGPPzMD9ri9zn; html5_pref=%7B%22SQ%22%3Afalse%2C%22MUTE%22%3Afalse%2C%22VOLUME%22%3A0.8518518518518519%2C%22FORCENOPICTURE%22%3Afalse%2C%22FORCENOAUTOBUFFER%22%3Afalse%2C%22FORCENATIVEHLS%22%3Afalse%2C%22PLAUTOPLAY%22%3Atrue%2C%22CHROMECAST%22%3Afalse%7D";
	public static final AtomicInteger counter = new AtomicInteger(0);
	static {
		File file = new File(VIDEODIR);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		Seimi s = new Seimi();
		s.start("xvideo");
	}

	@Override
	public String getUserAgent() {
		String[] uas = new String[] {
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36" };
		return uas[0];
	}

	@Override
	public String[] startUrls() {
		return new String[] { DOMAIN + "/tags/chinese" };
	}

	@Override
	public void start(Response response) {
		JXDocument doc = response.document();
		try {
			List<Object> divs = doc.sel(
					"//div[@class='mozaique']/div[@class='thumb-block ']/div[@class='thumb-inside']/div[@class='thumb']/script/text()");
			logger.info("{}", divs.size());
			for (Object s : divs) {
				try {
					String url = DOMAIN + getUrl(s.toString());
					String videoId = url.split("/")[3].replace("video", "");
					String tmpUrl = DOWNLOAD_URL + videoId + "/";
					push(new Request(tmpUrl, "parseData"));
				} catch (Exception e) {
					continue;
				}
			}
			List<Object> urls1 = doc.sel("//div[@class='pagination ']/ul/li/a/@href");
			for (Object url : urls1) {// 一个URL一个request
				try {
					int page = getNum(url.toString());
					if (page <= 0 || page > 0) {
						continue;
					}
				} catch (Exception e) {
					continue;
				}
				Request request = new Request();
				request.setCrawlerName(crawlerName);
				request.setUrl(DOMAIN + url.toString());
				request.setCallBack("start");// 回调Crawler 的start
				queue.push(request);// queue默认实现：DefaultLocalQueue
				logger.info("add {} url={} started", crawlerName, url);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static final String URL_REGX = "<a href=\"(/video\\d{8}/.{1,100})\">";

	public String getUrl(String content) throws Exception {
		Pattern p = Pattern.compile(URL_REGX);
		Matcher m = p.matcher(content);
		String url = "";
		if (m.find()) {
			url = m.group(1);
		}
		return url;
	}

	public int getNum(String url) throws Exception {
		String regEx = "[^0-9]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(url);
		int page = -1;
		try {
			page = Integer.valueOf(m.replaceAll("").trim());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return page;
	}

	public void parseData(Response response) {
		try {
			JSONObject json = JSONObject.parseObject(response.getContent());
			if (json.getBooleanValue("LOGGED")) {
				String src = json.getString("URL");
				if (StringUtils.isNotBlank(src)) {
					logger.info("video {} find", src);
					Request request = new Request();
					request.setCrawlerName(crawlerName);
					request.setUrl(src);
					request.setCallBack("parseVideo");
					request.setPriority(10);
					queue.push(request);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseVideo(Response response) {
		counter.incrementAndGet();
		try {
			String fileName = String.valueOf(counter.get()) + ".mp4";
			String path = VIDEODIR + fileName;
			if (!new File(path).exists()) {
				response.saveTo(new File(path));
				logger.info("file done = {}", fileName);
			}
		} catch (Exception e) {
		}
	}

	@Override
	public String cookies() {
		return COOKIES;
	}
}
