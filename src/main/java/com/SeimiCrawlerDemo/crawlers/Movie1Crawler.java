package com.SeimiCrawlerDemo.crawlers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.SeimiCrawlerDemo.CrawlerUtil;
import com.alibaba.fastjson.JSONObject;
import com.download.DLMain;
import com.download.DLTask;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.core.Seimi;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.xpath.model.JXDocument;

@Crawler(name = "mov", useCookie = true, delay = 2)
public class Movie1Crawler extends BaseSeimiCrawler {
	//public static final String VIDEODIR = Contants.VIDEODIR;
	public static final String VIDEODIR = "/Users/root/Desktop/videos";
	public static final String POST_URL = "http://www.alljavonline.com/plugins/gkpluginsphp.php";
	public static DLMain downloader;
	public static String startUrl="http://www.alljavonline.com/full-jav-online-11236/";
	@Override
	public String getUserAgent() {
		String[] uas = new String[] {
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36" };
		return uas[0];
	}

	static {
		File file = new File(VIDEODIR);
		if (!file.exists()) {
			file.mkdirs();
		}
		try {
			downloader = new DLMain();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		//startUrl=args[0];
		Seimi s = new Seimi();
		s.start("mov");
	}

	@Override
	public String[] startUrls() {
		execShow();
		return new String[] { startUrl };
	}

	@Override
	public void start(Response response) {
		JXDocument doc = response.document();
		try {
			String u = CrawlerUtil.getString(doc.sel("//div[@class='entry']/html()"));
			String link = u.substring(u.indexOf("link") + 6, u.indexOf("==") + 2);
			Request r = Request.build(POST_URL, "parseJson");
			r.setCrawlerName(crawlerName);
			Map<String, String> params = new HashMap<>();
			params.put("link", link);
			Map<String, String> header = new HashMap<String, String>();
			header.put("Referer", "http://www.alljavonline.com/full-jav-online-23838/");
			r.setHeader(header);
			r.setHttpMethod(HttpMethod.POST);
			r.setParams(params);
			queue.push(r);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseJson(Response response) {
		String json = response.getContent();
		JSONObject jo = JSONObject.parseObject(json);
		JSONObject req = jo.getJSONObject("request");
		String url = req.getString("url");
		Request request = new Request();
		Map<String, String> header = new HashMap<String, String>();
		header.put("Referer", "http://www.alljavonline.com/full-jav-online-23838/");
		request.setHeader(header);
		request.setCrawlerName(crawlerName);
		request.setUrl(url);
		request.setCallBack("parseContent");
		queue.push(request);
	}

	public void parseContent(Response response) {
		String content = response.getContent();
		// System.out.println(content);
		int start = content.indexOf("file:");
		int end = content.indexOf("v.mp4");
		String true_url = content.substring(start + 7, end + 5);
		logger.info("video {} find", true_url);
		download(true_url);
	}

	public void download(String url) {
		DLTask task = null;
		try {
			task = downloader.getTaskUriInfo(url, VIDEODIR);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (task == null) {
			System.out.println("获取文件信息失败，请稍后再试");
			return;
		}
		try {
			downloader.addTask(task);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("文件创建失败！" + task.getFile());
			return;
		}
		downloader.startTask(task);
		System.out.println("添加并开始任务：" + task.getName());
	}

	private Thread showInfoThread;

	/**
	 * execInfo的升级版：持续显示任务进度 <br/>
	 * 如果传入了taskid，则显示此任务的基本信息和分块下载信息 <br/>
	 * 如果没有传入taskid，则显示所有正在下载的任务的基本信息
	 */
	@SuppressWarnings("deprecation")
	private void execShow() {
		// 如果已经存在，则现停止
		if (showInfoThread != null) {
			showInfoThread.stop();
		}
		// 创建一个新的线程
		showInfoThread = new Thread("show-info-thread") {
			@Override
			public void run() {
				while (showInfoThread != null) {
					execInfo();
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		// 设置为后台线程
		showInfoThread.setDaemon(true);
		showInfoThread.start();
	}

	/**
	 * 显示任务进度 <br/>
	 * 如果传入了taskid，则显示此任务的基本信息和分块下载信息 <br/>
	 * 如果没有传入taskid，则显示所有正在下载的任务的基本信息
	 */
	private void execInfo() {
		// 获取正在下载的任务
		synchronized (downloader.getRunningTasks()) {
			System.out.println("===============================================");
			for (DLTask task : downloader.getRunningTasks()) {
				System.out.println(task);
			}
		}
	}
}
