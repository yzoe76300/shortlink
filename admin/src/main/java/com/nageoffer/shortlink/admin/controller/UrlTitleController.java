package com.nageoffer.shortlink.admin.controller;

import com.nageoffer.shortlink.admin.common.convension.result.Result;
import com.nageoffer.shortlink.admin.remote.ShortLinkRemoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/*
*  URL标题控制层
* */
@RestController
@RequiredArgsConstructor
public class UrlTitleController {

    private ShortLinkRemoteService service = new ShortLinkRemoteService(){};
    /*
    *  根据url获取标题
    * */
    @GetMapping("/api/short-link/admin/v1/title")
    public Result<String> getTitleByUrl(@RequestParam(value = "url") String url) throws IOException {
        return service.getTitleByUrl(url);
    }
}
