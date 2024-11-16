package com.nageoffer.shortlink.admin.controller;

import com.nageoffer.shortlink.admin.common.convension.result.Result;
import com.nageoffer.shortlink.admin.common.convension.result.Results;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortlinkGroupResponseDTO;
import com.nageoffer.shortlink.admin.service.IGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
* 短链接分组控制层
* */
@RestController
@RequiredArgsConstructor
public class GroupController {
    private final IGroupService groupService;

    /**
     * 新增短链接分组
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/v1/group")
    public Result<Void> save(@RequestBody ShortLinkGroupSaveReqDTO requestParam){
        groupService.saveGroup(requestParam.getName());
        return Results.success();

    }

    @GetMapping("/api/short-link/v1/group")
    public Result<List<ShortlinkGroupResponseDTO>> groupList(){
        List<ShortlinkGroupResponseDTO> groupList = groupService.getGroupList();
        return Results.success(groupList);
    }
}
