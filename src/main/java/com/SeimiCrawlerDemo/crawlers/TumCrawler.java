package com.SeimiCrawlerDemo.crawlers;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;

import com.SeimiCrawlerDemo.Contants;
import com.SeimiCrawlerDemo.CrawlerUtil;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.core.Seimi;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.def.PriorityLocalQueue;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.exception.XpathSyntaxErrorException;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "tum", useCookie = true, delay = 2, queue = PriorityLocalQueue.class)
public class TumCrawler extends BaseSeimiCrawler {
	public static final ConcurrentHashMap<String, Integer> pageMap = new ConcurrentHashMap<>();
	public static final String TRUE_VEDIO_URL = "https://vt.tumblr.com/%s.mp4#_=_";
	public static final AtomicInteger counter = new AtomicInteger(0);
	static {
		File file = new File(Contants.VIDEODIR);
		if (!file.exists()) {
			file.mkdirs();
		}
		File file1 = new File(Contants.IMAGEDIR);
		if (!file1.exists()) {
			file1.mkdirs();
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		Seimi s = new Seimi();
		s.start("tum");
	}

	@Override
	public String[] startUrls() {
		return new String[] { "http://szfcat.tumblr.com/post" };
	}

	@Override
	public void start(Response response) {
		JXDocument doc = response.document();
		try {
			List<Object> urls = doc.sel("//div[@class='tumblr_video_container']/iframe/@src");
			if (!urls.isEmpty()) {
				for (Object url : urls) {
					Request request = new Request();
					request.setCrawlerName(crawlerName);
					request.setUrl(url.toString());
					request.setCallBack("parseData");
					request.setPriority(6);
					queue.push(request);
				}
			} else if (response.getUrl().contains("page") && urls.isEmpty()) {
				logger.info("url[{}] is finished", response.getUrl());
				return;
			}
			List<Object> images = doc.sel("//div[@class='html_photoset']/iframe/@src");
			if (!images.isEmpty()) {
				for (Object url : images) {
					Request request = new Request();
					request.setCrawlerName(crawlerName);
					request.setUrl(url.toString());
					request.setCallBack("parseImage");
					request.setPriority(5);
					queue.push(request);
				}
			}
			List<Object> others = doc.sel("//a[@class='meta-item reblog-link']/@href");
			if (!others.isEmpty()) {
				for (Object other : others) {
					String url = other.toString();
					int endIndex = url.indexOf(".com");
					if (endIndex > -1) {
						String _url = url.substring(0, endIndex + 4);
						Request request = new Request();
						request.setCrawlerName(crawlerName);
						request.setUrl(_url);
						request.setCallBack("start");
						queue.push(request);
						logger.info("add {} url={} started", crawlerName, _url);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		String url = "";
		if (!response.getUrl().contains("/page/")) {
			url = response.getUrl();
			pageMap.put(response.getUrl(), 2);
		} else {
			url = response.getUrl().substring(0, response.getUrl().lastIndexOf("page") - 1);
			pageMap.put(url, pageMap.get(url) + 1);
		}
		url = url + "/page/" + pageMap.get(url);
		Request request = new Request();
		request.setCrawlerName(crawlerName);
		request.setUrl(url);
		request.setCallBack("start");// 回调Crawler 的start
		queue.push(request);// queue默认实现：DefaultLocalQueue
		logger.info("add {} url={} started", crawlerName, url);
	}

	public void parseImage(Response response) {
		JXDocument doc = response.document();
		try {
			List<Object> imageUrls = doc.sel("//a[@class='photoset_photo rapid-noclick-resp']/@href");
			if (!imageUrls.isEmpty()) {
				for (Object url : imageUrls) {
					Request request = new Request();
					request.setCrawlerName(crawlerName);
					request.setUrl(url.toString());
					request.setCallBack("imageSave");// 回调Crawler 的start
					request.setPriority(9);
					queue.push(request);// queue默认实现：DefaultLocalQueue
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseData(Response response) {
		JXDocument doc = response.document();
		try {
			String src = CrawlerUtil.getString(doc.sel("//video[@class='crt-video crt-skin-default']/source/@src"));
			if (StringUtils.isNotBlank(src)) {
				logger.info("video {} find", src);
				Request request = new Request();
				request.setCrawlerName(crawlerName);
				request.setUrl(String.format(TRUE_VEDIO_URL, src.substring(src.lastIndexOf("/") + 1)));
				request.setCallBack("parseVideo");
				request.setPriority(10);
				queue.push(request);
			}
		} catch (XpathSyntaxErrorException e) {
			e.printStackTrace();
		}
	}

	public void parseVideo(Response response) {
		counter.incrementAndGet();
		try {
			//String fileName = StringUtils.substringAfterLast(response.getUrl().replace("#_=_",""), "/");
			String fileName = String.valueOf(counter.get())+".mp4";
			String path = Contants.VIDEODIR + fileName;
			if (!new File(path).exists()) {
				response.saveTo(new File(path));
				logger.info("file done = {}", fileName);
			}
		} catch (Exception e) {
		}
	}

	public void imageSave(Response response) {
		try {
			String fileName = StringUtils.substringAfterLast(response.getUrl(), "/");
			String path = Contants.IMAGEDIR + fileName;
			if (!new File(path).exists()) {
				response.saveTo(new File(path));
			}
		} catch (Exception e) {
		}
	}
}
