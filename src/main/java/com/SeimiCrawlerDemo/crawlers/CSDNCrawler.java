package com.SeimiCrawlerDemo.crawlers;

import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;

import com.SeimiCrawlerDemo.CrawlerUtil;
import com.SeimiCrawlerDemo.dao.StoreToDbDAO;
import com.SeimiCrawlerDemo.model.CsdnBlog;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.core.Seimi;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "csdn")
public class CSDNCrawler extends BaseSeimiCrawler {
	@Autowired
	private StoreToDbDAO storeToDbDAO;
	public static final String BASEURL = "http://blog.csdn.net";
	public boolean PAGER = true;

	public String proxy1() {
		String[] proxies = new String[] { "http://47.88.104.219:80", "http://141.85.220.108:8080",
				"http://46.48.236.161:3128" };
		return proxies[RandomUtils.nextInt() % proxies.length];
	}

	@Override
	public String getUserAgent() {
		String[] uas = new String[] {
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36" };
		return uas[0];
	}

	public static void main(String[] args) {
		try (Seimi s = new Seimi()) {
			s.start("csdn");
		}
	}

	@Override
	public String[] startUrls() {
		return new String[] { "http://blog.csdn.net/csh624366188/article" };
	}

	@Override
	public void start(Response response) {
		JXDocument doc = response.document();
		try {
			List<Object> blogs = doc
					.sel("//div[@class='list_item article_item']/div[@class='article_title']/h1/span/a");
			for (Object blog : blogs) {
				Element e = (Element) blog;
				Request request = new Request();
				request.setCrawlerName(crawlerName);
				request.setUrl(BASEURL + e.attr("href"));
				request.setCallBack("parseContent");
				request.setPriority(1);
				queue.push(request);
				logger.info("add {} url={} started", crawlerName, request.getUrl());
			}
			Element pager = CrawlerUtil.getFirstElement(doc.sel("//div[@id='papelist']"));
			Element first = pager.getElementsByTag("strong").first();
			if (PAGER && first.nextElementSibling() != null) {
				Element next = first.nextElementSibling();
				String url = BASEURL + next.attr("href");
				Request request = new Request();
				request.setCrawlerName(crawlerName);
				request.setUrl(url.toString());
				request.setCallBack("start");
				request.setPriority(0);
				queue.push(request);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void parseContent(Response response) {
		JXDocument doc = response.document();
		try {
			int blogId = Integer.valueOf(response.getUrl().split("/")[6]);
			String userId = response.getUrl().split("/")[3];
			String title = CrawlerUtil.getString(doc.sel("//div[@class='article_title']/h1/span/a/text()"));
			String content = CrawlerUtil
					.getPlainText(CrawlerUtil.getString(doc.sel("//div[@class='article_content']/html()")));
			String createStr = CrawlerUtil.getString(doc.sel("//span[@class='link_postdate']/text()"));
			String viewStr = CrawlerUtil.getString(doc.sel("//span[@class='link_view']/text()")).replace("人阅读", "");
			CsdnBlog blog = new CsdnBlog();
			blog.setBlogId(blogId);
			blog.setUserId(userId);
			blog.setContent(content);
			blog.setCreateTime(CrawlerUtil.parseDate(createStr));
			blog.setTitle(title);
			blog.setViewCount(CrawlerUtil.parseInt(viewStr));
			int id = storeToDbDAO.save(blog);
			System.out.println(id);
		} catch (Exception e) {
			logger.error("{},url={} exception:{}", crawlerName, response.getUrl(), e.getMessage());
		}
	}

}
