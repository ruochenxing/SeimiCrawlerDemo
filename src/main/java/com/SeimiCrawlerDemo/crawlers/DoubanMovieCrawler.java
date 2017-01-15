package com.SeimiCrawlerDemo.crawlers;

import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.SeimiCrawlerDemo.dao.StoreToDbDAO;
import com.SeimiCrawlerDemo.model.DoubanMovie;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "doubanmovie")
public class DoubanMovieCrawler extends BaseSeimiCrawler {
	@Autowired
	private StoreToDbDAO storeToDbDAO;

	@Override
	public String[] startUrls() {
		return new String[] { "https://movie.douban.com/tag/%E7%88%B1%E6%83%85?type=S" };
	}

    @Override
    public String proxy() {
        String[] proxies = new String[]{"http://47.88.104.219:80","http://141.85.220.108:8080","http://46.48.236.161:3128"};
        return proxies[RandomUtils.nextInt()%proxies.length];
    }

	@Override
	public void start(Response response) {
		JXDocument doc = response.document();
		try {
			List<Object> urls = doc.sel("//div[@class='pl2']/a/@href");
			logger.info("{}", urls.size());
			for (Object s : urls) {
				push(new Request(s.toString(), "parseData"));
			}

			List<Object> urls1 = doc.sel("//div[@class='paginator']/a/@href");
			for (Object url : urls1) {// 一个URL一个request
				Request request = new Request();
				request.setCrawlerName(crawlerName);
				request.setUrl(url.toString());
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
			DoubanMovie movie = response.render(DoubanMovie.class);
			logger.info("bean resolve res={},url={}", movie, response.getUrl());
			int id = storeToDbDAO.save(movie);
			logger.info("store sus,movieId = {}", id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
