package cn.wanghaomiao.seimi.def;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.wanghaomiao.seimi.annotation.Queue;
import cn.wanghaomiao.seimi.core.SeimiQueue;
import cn.wanghaomiao.seimi.struct.Request;

@Queue
public class PriorityLocalQueue implements SeimiQueue {
	private Map<String, PriorityBlockingQueue<Request>> queueMap = new HashMap<>();
	private Map<String, ConcurrentSkipListSet<String>> processedData = new HashMap<>();
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public Request bPop(String crawlerName) {
		try {
			PriorityBlockingQueue<Request> queue = getQueue(crawlerName);
			return queue.take();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean push(Request req) {
		PriorityBlockingQueue<Request> queue = getQueue(req.getCrawlerName());
		queue.put(req);
		return true;
	}

	@Override
	public long len(String crawlerName) {
		PriorityBlockingQueue<Request> queue = getQueue(crawlerName);
		return queue.size();
	}

	@Override
	public boolean isProcessed(Request req) {
		ConcurrentSkipListSet<String> set = getProcessedSet(req.getCrawlerName());
		String sign = DigestUtils.md5Hex(req.getUrl());
		return set.contains(sign);
	}

	@Override
	public void addProcessed(Request req) {
		ConcurrentSkipListSet<String> set = getProcessedSet(req.getCrawlerName());
		String sign = DigestUtils.md5Hex(req.getUrl());
		set.add(sign);
	}

	@Override
	public long totalCrawled(String crawlerName) {
		ConcurrentSkipListSet<String> set = getProcessedSet(crawlerName);
		return set.size();
	}

	public PriorityBlockingQueue<Request> getQueue(String crawlerName) {
		PriorityBlockingQueue<Request> queue = queueMap.get(crawlerName);
		if (queue == null) {
			queue = new PriorityBlockingQueue<>();
			queueMap.put(crawlerName, queue);
		}
		return queue;
	}

	public ConcurrentSkipListSet<String> getProcessedSet(String crawlerName) {
		ConcurrentSkipListSet<String> set = processedData.get(crawlerName);
		if (set == null) {
			set = new ConcurrentSkipListSet<>();
			processedData.put(crawlerName, set);
		}
		return set;
	}
}
