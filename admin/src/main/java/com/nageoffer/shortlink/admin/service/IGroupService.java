package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSortGroupReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortlinkGroupResponseDTO;

import java.util.List;

/**
 * 短链接分组接口层
 */
public interface IGroupService extends IService<GroupDO> {
    /*
    * 新增短链接分组
    * */
    public void saveGroup(String userName, String groupName);
    /*
    * 新增短链接分组
    * */
    public void saveGroup(String groupName);
    /*
    * 获取短链接分组列表
    * */
    List<ShortlinkGroupResponseDTO> getGroupList();
    /*
    * 更新短链接分组详情
    * */
    void updateGroup(ShortLinkGroupUpdateReqDTO requestParam);
    /*
    * 删除短链接分组标识
    * */
    void deleteGroup(String gid);
    /*
    * 排序短链接分组
    * */
    void sortGroup(List<ShortLinkGroupSortGroupReqDTO> requestParam);
}
