package com.SeimiCrawlerDemo;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class CrawlerUtil {
	public static String getString(List<Object> values) {
		if (values == null || values.isEmpty()) {
			return "";
		} else {
			return String.valueOf(values.get(0));
		}
	}

	public static int getInt(List<Object> values) {
		int result = -99999;
		if (values == null || values.isEmpty()) {
			return result;
		} else {
			String temp = String.valueOf(values.get(0));
			return parseInt(temp);
		}
	}

	public static int parseInt(String value) {
		int result = -99999;
		try {
			result = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return -99999;
		}
		return result;
	}

	public static Timestamp parseDate(String value) {
		try {
			return new Timestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(value).getTime());
		} catch (ParseException e) {
			return null;
		}
	}

	public static Element getFirstElement(List<Object> values) {
		if (values == null || values.isEmpty()) {
			return null;
		} else {
			return (Element) values.get(0);
		}
	}

	/**
	 * 从一段HTML中萃取纯文本
	 * 
	 * @param html
	 * @return
	 */
	public static String getPlainText(String html) {
		if (StringUtils.isBlank(html))
			return "";
		return Jsoup.parse(html).text();
	}
}
