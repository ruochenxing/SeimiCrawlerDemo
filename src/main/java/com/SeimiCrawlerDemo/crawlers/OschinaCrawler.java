package com.SeimiCrawlerDemo.crawlers;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.core.Seimi;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "osc", useCookie = true, delay = 3)
public class OschinaCrawler extends BaseSeimiCrawler {
	public static final String PREFIX_URL = "https://my.oschina.net/javayou/home?type=tweet&scope=entity&showme=SHOW&temp=1479286457939&p=";
	public static final String BASE_URL = "https://my.oschina.net";

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		Seimi s = new Seimi();
		s.start("osc");
	}

	@Override
	public String getUserAgent() {
		String[] uas = new String[] {
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36" };
		return uas[0];
	}

	@Override
	public String[] startUrls() {
		return new String[] { PREFIX_URL + "1" };
	}

	@Override
	public void start(Response response) {
		JXDocument doc = response.document();
		try {
			List<Object> urls = doc.sel(
					"//div[@class='layout-flex tweet-item clearfix']/div[@class='flex-grow tweet-body']/div[@class='user-title']");
			for (Object s : urls) {
				Element e = (Element) s;
				Elements elements = e.children();
				if (elements.size() == 3) {
					elements.stream().forEach((Element ee) -> {
						System.out.print(ee.text() + "\t");
					});
					Element detail=e.nextElementSibling();
					if(StringUtils.isEmpty(detail.child(1).text())){
						System.out.println("[img]");
					}
					else{
						System.out.println(detail.child(1).text());
					}
				}
			}
			List<Object> urls1 = doc.sel("//div[@class='pages']/ul/li[@class='active']/a/@href");
			for (Object url : urls1) {// 一个URL一个request
				String u = url.toString();
				int page = Integer.parseInt(u.substring(u.lastIndexOf("=") + 1)) + 1;
				if (page > 9) {
					break;
				}
				Request request = new Request();
				request.setCrawlerName(crawlerName);
				request.setUrl(PREFIX_URL + page);
				request.setCallBack("start");// 回调Crawler 的start
				queue.push(request);// queue默认实现：DefaultLocalQueue
				//logger.info("add {} url={} started", crawlerName, url);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
