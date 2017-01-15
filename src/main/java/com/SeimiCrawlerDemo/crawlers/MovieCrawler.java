package com.SeimiCrawlerDemo.crawlers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.SeimiCrawlerDemo.Contants;
import com.SeimiCrawlerDemo.CrawlerUtil;
import com.alibaba.fastjson.JSONArray;
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

@Crawler(name = "movie", useCookie = true, delay = 2)
public class MovieCrawler extends BaseSeimiCrawler {
	public static final ConcurrentHashMap<String, Integer> pageMap = new ConcurrentHashMap<>();
	public static final String VIDEODIR = Contants.VIDEODIR;
	public static DLMain downloader;
	private Thread showInfoThread;
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
		Seimi s = new Seimi();
		s.start("movie");
	}

	@Override
	public String[] startUrls() {
		execShow();
		return new String[] { "http://javleak.com/director/s-cute/" };
	}

	@Override
	public void start(Response response) {
		JXDocument doc = response.document();
		try {
			List<Object> movies = doc.sel("//div[@id='box_movies']/div[@class='movie']/div[@class='imagen']/a/@href");
			if (!movies.isEmpty()) {
				for (Object movie : movies) {
					logger.info("video {} find", movie.toString());
					Request request = new Request();
					request.setCrawlerName(crawlerName);
					request.setUrl(movie.toString());
					request.setCallBack("parseContent");
					queue.push(request);
				}
			} else {
				logger.info("url[{}] is finished", response.getUrl());
				return;
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseContent(Response response) {
		JXDocument doc = response.document();
		try {
			String videoIFrameUrl = CrawlerUtil.getString(doc.sel("//div[@id='play-1']/iframe/@src"));
			if (StringUtils.isBlank(videoIFrameUrl)) {
				videoIFrameUrl = CrawlerUtil.getString(doc.sel("//div[@id='play-2']/iframe/@src"));
				if (StringUtils.isBlank(videoIFrameUrl)) {
					logger.warn("请求失败:url={},content={}", response.getUrl(), response.getContent());
					return;
				}
			}
			Request req = Request.build(videoIFrameUrl, "parseVideo");
			Map<String, String> params = new HashMap<>();
			params.put("confirm.x", "71");
			params.put("confirm.y", "72");
			params.put("block", "1");
			Map<String, String> header = new HashMap<>();
			header.put("Referer", videoIFrameUrl);
			req.setHeader(header);
			req.setHttpMethod(HttpMethod.POST);
			req.setParams(params);
			req.setCrawlerName(crawlerName);
			queue.push(req);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseVideo(Response response) {
		if (!response.getContent().contains(".setup(")) {
			logger.warn("请求失败:url={}", response.getUrl());
			return;
		}
		Pattern p = Pattern.compile("\\.setup\\((.*?})\\);");
		Matcher matcher = p.matcher(response.getContent());
		String json = null;
		while (matcher.find()) {
			json = matcher.group(1);
		}
		if (json == null) {
			logger.warn("匹配失败,html={}", response.getUrl());
			return;
		}
		JSONObject jsonObject = JSONObject.parseObject(json);
		if (jsonObject == null) {
			logger.warn("匹配失败,json={}", json);
			return;
		}
		JSONArray array = jsonObject.getJSONArray("sources");
		if (array == null || array.size() <= 0) {
			logger.warn("匹配失败,json={}", json);
			return;
		}
		JSONObject o = JSONObject.parseObject(array.get(0).toString());
		String url = o.getString("file");
		logger.info("video {} find", url);
		download(url);
		// Request request = new Request();
		// request.setCrawlerName(crawlerName);
		// request.setUrl(url);
		// request.setCallBack("saveVideo");
		// queue.push(request);
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

	@Deprecated
	public void saveVideo(Response response) {
		System.out.println("saveVideo");
		try {
			String fileName = StringUtils.substringAfterLast(response.getUrl(), "/");
			String path = VIDEODIR + fileName;
			if (!new File(path).exists()) {
				response.saveToInProgressBar(new File(path));
				logger.info("file done = {}", fileName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
