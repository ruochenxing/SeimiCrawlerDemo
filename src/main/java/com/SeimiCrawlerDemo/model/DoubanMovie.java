package com.SeimiCrawlerDemo.model;

import cn.wanghaomiao.seimi.annotation.Xpath;

public class DoubanMovie {
    
	@Xpath("//div[@id='content']/h1/span/text()")
    private String title;
    
    @Xpath("//strong[@class='ll rating_num']/text()")
    private String rateResult;
    
    @Xpath("//a[@class='rating_people']/span/text()")
    private String rateNum;
    //也可以这么写 @Xpath("//div[@id='cnblogs_post_body']//text()")

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRateResult() {
		return rateResult;
	}

	public void setRateResult(String rateResult) {
		this.rateResult = rateResult;
	}

	public String getRateNum() {
		return rateNum;
	}

	public void setRateNum(String rateNum) {
		this.rateNum = rateNum;
	}

	@Override
	public String toString() {
		return "DoubanMovie [title=" + title + ", rateResult=" + rateResult + ", rateNum=" + rateNum + "]";
	}
}


