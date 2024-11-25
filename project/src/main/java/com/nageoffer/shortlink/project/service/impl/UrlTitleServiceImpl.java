package com.nageoffer.shortlink.project.service.impl;

import com.nageoffer.shortlink.project.service.UrlTitleService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/*
 *  URL标题接口实现层
 * */
@Service
public class UrlTitleServiceImpl implements UrlTitleService {


    @Override
    public String getTitleByUrl(String url) throws IOException {
        URL tagetUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) tagetUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Document document = Jsoup.connect(url).get();
            return document.title();
        }
        return "Erro while fetching title";

    }
}
