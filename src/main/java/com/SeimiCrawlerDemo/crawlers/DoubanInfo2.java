package com.SeimiCrawlerDemo.crawlers;

import java.util.List;

import org.jsoup.nodes.Element;

import com.SeimiCrawlerDemo.CrawlerUtil;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "douban", useCookie = true, delay = 5)
public class DoubanInfo2 extends BaseSeimiCrawler {
	private static final String COOKIES = "";

	@Override
	public String[] startUrls() {
		return new String[] { "https://www.douban.com/group/haixiuzu/discussion?start=0" };
	}

	@Override
	public String getUserAgent() {
		String[] uas = new String[] {
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36" };
		return uas[0];
	}

	@Override
	public void start(Response response) {
		if (!response.getContent().contains("若星")) {
			System.out.println("登录失败");
			System.exit(0);
			return;
		}
		// System.out.println(response.getContent());
		JXDocument doc = response.document();
		try {
			List<Object> urls = doc.sel("//td[@class='title']/a/@href");
			logger.info("{}", urls.size());
			for (Object s : urls) {
				push(new Request(s.toString(), "parseDisc"));
			}
			List<Object> urls1 = doc.sel("//div[@class='paginator']/a/@href");
			for (Object url : urls1) {// 一个URL一个request
				String u = url.toString();
				int start = Integer.parseInt(u.substring(u.lastIndexOf("=") + 1));
				if (start <= 200) {
					Request request = new Request();
					request.setCrawlerName(crawlerName);
					request.setUrl(u);
					request.setCallBack("start");// 回调Crawler 的start
					queue.push(request);// queue默认实现：DefaultLocalQueue
					logger.info("add {} url={} started", crawlerName, url);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseDisc(Response response) {
		// System.out.println(response.getContent());
		try {
			JXDocument doc = response.document();
			List<Object> urls = doc.sel("//span[@class='from']/a");
			if (!urls.isEmpty()) {
				Element element = (Element) urls.get(0);
				push(new Request(element.attr("href"), "parseInfo"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseInfo(Response response) {
		// System.out.println(response.getContent());
		try {
			JXDocument doc = response.document();
			String name = CrawlerUtil.getString(doc.sel("//div[@class='info']/h1/text()"));
			String joinTime = CrawlerUtil.getString(doc.sel("//div[@class='user-info']/div/text()"));
			List<Object> locations = doc.sel("//div[@class='user-info']/a/text()");
			List<Object> friends = doc.sel("//div[@class='aside']/div/h2/span[@class='pl']/a/text()");
			String intro = CrawlerUtil
					.getString(doc.sel("//div[@class='user-intro']/div/span[@id='intro_display']/text()"));
			int friendNum = 0;
			if (!friends.isEmpty()) {
				String value = friends.get(0).toString();
				value = value.replace("成员", "");
				friendNum = Integer.parseInt(value);
			}
			System.out.println(name + "\t" + joinTime + "\t常居:" + CrawlerUtil.getString(locations) + "\t" + friendNum
					+ "\t" + response.getUrl());
			System.out.println(intro);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String cookies() {
		return COOKIES;
	}
}
