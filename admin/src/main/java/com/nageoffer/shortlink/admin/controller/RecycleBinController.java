package com.nageoffer.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convension.result.Result;
import com.nageoffer.shortlink.admin.common.convension.result.Results;
import com.nageoffer.shortlink.admin.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shortlink.admin.remote.ShortLinkRemoteService;
import com.nageoffer.shortlink.admin.remote.dto.req.RecycleBinDeleteReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.admin.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
*  回收站控制层
* */
@RequiredArgsConstructor
@RestController
public class RecycleBinController {

    private final ShortLinkRemoteService service = new ShortLinkRemoteService(){};
    private final RecycleBinService recycleBinService;

    /*
    * 远程保存回收站短链
    * */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam){
        service.saveRecycleBin(requestParam);
        return Results.success();
    }

    /*
     * 远程查询回收站短链分页数据
     * */
    @GetMapping("/api/short-link/admin/v1/recycle-bin/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageRecycleBin(ShortLinkRecycleBinPageReqDTO requestParam) {
        return recycleBinService.pageRecycleBin(requestParam);
    }

    /*
     * 远程查恢复收站短链
     * */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/recover")
    public Result<Void> recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam) {
        service.recoverRecycleBin(requestParam);
        return Results.success();
    }

    /**
     * 远程删除回收站短链
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/remove")
    public Result<Void> removeRecycleBin(@RequestBody RecycleBinDeleteReqDTO requestParam){
        service.deleteRecycleBin(requestParam);
        return Results.success();
    }
}
