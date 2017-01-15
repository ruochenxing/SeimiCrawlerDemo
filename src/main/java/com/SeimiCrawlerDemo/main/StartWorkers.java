package com.SeimiCrawlerDemo.main;

import cn.wanghaomiao.seimi.core.Seimi;

public class StartWorkers {
    @SuppressWarnings("resource")
	public static void main(String[] args){
        Seimi s = new Seimi();
        s.startWorkers();
    }
}
