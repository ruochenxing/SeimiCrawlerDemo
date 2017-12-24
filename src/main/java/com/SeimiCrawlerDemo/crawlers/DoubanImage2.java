package com.SeimiCrawlerDemo.crawlers;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.SeimiCrawlerDemo.Contants;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "douban1", useCookie = true,delay = 3)
public class DoubanImage2 extends BaseSeimiCrawler {
	private static final String COOKIES="";
	public static final String IMAGEDIR = Contants.IMAGEDIR;
	
	static {
		File file = new File(IMAGEDIR);
		if (!file.exists()) {
			file.mkdirs();
		}
	}
	
	@Override
	public String[] startUrls() {
		//return new String[] {""};
		return null;
	}

	@Override
	public String getUserAgent() {
		String[] uas = new String[] {
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36"};
		return uas[0];
	}
	
	@Override
	public List<Request> startRequests(){
		List<Request> requests = new LinkedList<>();
        Request start = Request.build("https://www.douban.com/accounts/login","start");
        Map<String,String> params = new HashMap<>();
        params.put("source","index_nav");
        params.put("form_email","sherlock_65534@qq.com");
        params.put("form_password","password");
        params.put("remember","on");
        start.setHttpMethod(HttpMethod.POST);
        start.setParams(params);
        requests.add(start);
        return requests;
	}
	@Override
	public void start(Response response) {
		System.out.println(response.getContent());
		if(response.getContent().contains("我的豆瓣")){
			System.out.println("登录成功");
		}
		else if(response.getContent().contains("请输入上图中的单词")){
			System.out.println("需要验证码登录");
			System.exit(0);
		}
		else{
			System.out.println("登录失败");
		}
		push(Request.build("https://www.douban.com/group/haixiuzu/discussion?start=50", "mainPage"));
	}

	public void mainPage(Response response) {
		JXDocument doc = response.document();
		try {
			List<Object> urls = doc.sel("//td[@class='title']/a/@href");
			logger.info("{}", urls.size());
			for (Object s : urls) {
				push(new Request(s.toString(), "parseData"));
			}

			List<Object> urls1 = doc.sel("//div[@class='paginator']/a/@href");
			for (Object url : urls1) {// 一个URL一个request
				String u=url.toString();
				int start=Integer.parseInt(u.substring(u.lastIndexOf("=")+1));
				if(start<=200){
					Request request = new Request();
					request.setCrawlerName(crawlerName);
					request.setUrl(u);
					request.setCallBack("mainPage");// 回调Crawler 的start
					queue.push(request);// queue默认实现：DefaultLocalQueue
					logger.info("add {} url={} started", crawlerName, url);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseData(Response response) {
		try {
			JXDocument doc = response.document();
			List<Object> urls = doc.sel("//div[@class='topic-figure cc']/img/@src");
			logger.info("图片数量：{}", urls.size());
			for (Object s : urls) {
				push(new Request(s.toString(), "saveFile"));
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
	public String cookies(){
		return COOKIES;
	}
}
