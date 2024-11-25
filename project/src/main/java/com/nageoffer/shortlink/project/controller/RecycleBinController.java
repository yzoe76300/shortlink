package com.nageoffer.shortlink.project.controller;

import com.nageoffer.shortlink.project.common.convension.result.Result;
import com.nageoffer.shortlink.project.common.convension.result.Results;
import com.nageoffer.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
*  回收站控制层
* */
@RequiredArgsConstructor
@RestController
public class RecycleBinController {
    private final RecycleBinService recycleBinService;

    /*
    * 保存回收站
    * */
    @PostMapping("/api/short-link/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam){
        recycleBinService.saveRecycleBin(requestParam);
        return Results.success();
    }
}
