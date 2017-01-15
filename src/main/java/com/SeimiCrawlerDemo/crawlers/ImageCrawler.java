package com.SeimiCrawlerDemo.crawlers;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.SeimiCrawlerDemo.Contants;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "image", useCookie = true, delay = 1)
public class ImageCrawler extends BaseSeimiCrawler {
	public static final String DOMAIN = "http://110av.com/";
	public static final String IMAGEDIR = Contants.IMAGEDIR;
	private static final String COOKIES = "";
	static {
		File file = new File(IMAGEDIR);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	@Override
	public String[] startUrls() {
		return new String[] { DOMAIN + "/html/tupian/toupai/index_2.html" };
	}

	@Override
	public String getUserAgent() {
		String[] uas = new String[] {
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36" };
		return uas[0];
	}

	@Override
	public void start(Response response) {
		JXDocument doc = response.document();
		try {
			List<Object> urls = doc.sel("//div[@class='art']/ul/li/a/@href");
			logger.info("{}", urls.size());
			for (Object s : urls) {
				push(new Request(DOMAIN + s.toString(), "parseData"));
			}

			List<Object> urls1 = doc.sel("//div[@id='pages']/a/@href");
			for (Object url : urls1) {// 一个URL一个request
				if ((DOMAIN + "/html/tupian/toupai/index.html").equals(url.toString())) {
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

	public void parseData(Response response) {
		try {
			JXDocument doc = response.document();
			List<Object> urls = doc.sel("//div[@class='artbody imgbody']/p/img/@src");
			logger.info("图片数量：{}", urls.size());
			for (Object s : urls) {
				push(new Request("http:" + s.toString(), "saveFile"));
			}
			logger.info("url:{} {}", response.getUrl(), doc.sel("//div[@id='content']/h1/text()"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveFile(Response response) {
		try {
			String fileName = StringUtils.substringAfterLast(response.getUrl(), "/");
			String path = IMAGEDIR + fileName;
			response.saveTo(new File(path));
			logger.info("file done = {}", fileName);
		} catch (Exception e) {
		}
	}

	@Override
	public String cookies() {
		return COOKIES;
	}
}
