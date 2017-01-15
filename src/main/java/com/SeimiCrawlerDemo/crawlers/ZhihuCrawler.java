package com.SeimiCrawlerDemo.crawlers;

import java.util.List;

import com.SeimiCrawlerDemo.CrawlerUtil;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "zhihu", useCookie = true, delay = 3)
public class ZhihuCrawler extends BaseSeimiCrawler {
	private static final String COOKIES = "d_c0=\"AEAApRn4dwqPTqKl3y3S1G80O0PkA7J17OI=|1472629684\"; _zap=f4b8890b-7a39-493e-beb8-447138c8c8a8; _za=ce0f3ef3-ce61-41b3-a3c8-c3544f8bf7f5; l_cap_id=\"MjU2MjEzMzQxMTdlNDQ5Yzg4NjcyZTk5N2JmYjg2OTQ=|1475221861|c45dbc76143b7d906419deba2a32047c524a3ff7\"; cap_id=\"OWJmN2Q1MDU4YTc1NGY1NDlhZDc3YzgzMjJmMjQyYTM=|1475221861|9522512d6f7aee15be771a39c0c5a2d3630486fa\"; login=\"NGNhYWU3ZDNiZGU0NGFhYmEyOGY5MjBmZjFjNWJhMmY=|1475221874|9d16ca0bc5c329bade03e472c4858a79973b3b16\"; q_c1=032ce98bf61e467aa2fa7d8d484104d5|1475893250000|1472629683000; _xsrf=804c2ce898107ef8d4fe97d7e66057b6; a_t=\"2.0AADAJH0bAAAXAAAA-mclWAAAwCR9GwAAAEAApRn4dwoXAAAAYQJVTXKmFVgAHh9G9zKSA_w50ic5of9yQZMrRPU6Cgt7h80ZVe5LKVE74E5Q10piZA==\"; z_c0=Mi4wQUFEQUpIMGJBQUFBUUFDbEdmaDNDaGNBQUFCaEFsVk5jcVlWV0FBZUgwYjNNcElEX0RuU0p6bWhfM0pCa3l0RTlR|1476254458|dbbdafee44b054c46cc44aa68a938d34707e5c9c; __utmt=1; __utma=51854390.1435435297.1476065949.1476243511.1476254458.9; __utmb=51854390.2.10.1476254458; __utmc=51854390; __utmz=51854390.1476065949.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); __utmv=51854390.100-1|2=registration_date=20130520=1^3=entry_date=20130520=1";

	/**
	 * 默认调用start方法
	 */
	@Override
	public String[] startUrls() {
		return new String[] { "https://www.zhihu.com/people/ruo/followees" };
	}

	@Override
	public String getUserAgent() {
		String[] uas = new String[] {
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36" };
		return uas[0];
	}

	@Override
	public List<Request> startRequests() {
		return null;
	}

	public void mainIndex(Response response) {
	}

	@Override
	public void start(Response response) {
		JXDocument doc = response.document();
		try {
			String name = CrawlerUtil.getString(doc.sel("//div[@class='title-section']/a[@class='name']/text()"));
			String desc = CrawlerUtil.getString(doc.sel("//div[@class='title-section']/div[@class='bio ellipsis']/text()"));
			int followee = CrawlerUtil.getInt(doc.sel("//div[@class='zm-profile-side-following zg-clear']/a[1]/strong/text()"));
			int follower = CrawlerUtil.getInt(doc.sel("//div[@class='zm-profile-side-following zg-clear']/a[2]/strong/text()"));
			String location = CrawlerUtil.getString(doc.sel("//span[@class='location item']/a/text()"));
			int agreeNum = CrawlerUtil.getInt(doc.sel("//span[@class='zm-profile-header-user-agree']/strong/text()"));
			int thanks = CrawlerUtil.getInt(doc.sel("//span[@class='zm-profile-header-user-thanks']/strong/text()"));
			String sex = CrawlerUtil.getString(doc.sel("//span[@class='item gender']/i/@class"));
			System.out.println(name + "\t" + desc + "\t" + "关注人数：" + followee + "人\t粉丝数：" + follower + "\t城市："
					+ location + "\t赞同数：" + agreeNum + "\t感谢：" + thanks + "\t性别：" + sex);
			List<Object> urls = doc.sel("//span[@class='author-link-line']/a/@href");
			for (Object s : urls) {
				Request request = new Request();
				request.setCrawlerName(crawlerName);
				request.setUrl(s.toString() + "/followees");
				request.setCallBack("start");// 回调Crawler 的start
				Thread.sleep(1000);
				queue.push(request);// queue默认实现：DefaultLocalQueue
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String cookies() {
		return COOKIES;
	}
}
