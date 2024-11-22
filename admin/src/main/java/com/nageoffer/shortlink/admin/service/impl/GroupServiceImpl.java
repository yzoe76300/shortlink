package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.convension.result.Result;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSortGroupReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortlinkGroupResponseDTO;
import com.nageoffer.shortlink.admin.remote.ShortLinkRemoteService;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkCountQueryRespDTO;
import com.nageoffer.shortlink.admin.service.IGroupService;
import com.nageoffer.shortlink.admin.toolkit.RandomGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 短链接分组接口业务实现层
 */
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl <GroupMapper, GroupDO> implements IGroupService {

    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService(){

    };
    @Override
    public void saveGroup(String groupName) {
        this.saveGroup(UserContext.getUsername(), groupName);
    }

    @Override
    public void saveGroup(String userName,String groupName) {
        // 检查是否存在同名gid
        String gid;
        do {
            gid = RandomGenerator.generateRandomString();
        } while(hasGid(userName,gid));
        // 不存在则生成新的gid
        GroupDO groupDO = GroupDO.builder()
                .gid(RandomGenerator.generateRandomString())
                .sortOrder(0)
                .username(userName)
                .name(groupName)
                .build();
        baseMapper.insert(groupDO);
    }

    @Override
    public List<ShortlinkGroupResponseDTO> getGroupList() {
        LambdaQueryWrapper<GroupDO> groupDOLambdaQueryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOList = baseMapper.selectList(groupDOLambdaQueryWrapper);
        Result<List<ShortLinkCountQueryRespDTO>> listResult = shortLinkRemoteService
                .listGroupShortLink(groupDOList
                        .stream()
                        .map(GroupDO::getGid)
                        .collect(Collectors.toList()));
        List<ShortlinkGroupResponseDTO> shortlinkGroupRespDTOList = BeanUtil.copyToList(groupDOList, ShortlinkGroupResponseDTO.class);
        shortlinkGroupRespDTOList.forEach(each -> {
            String gid = each.getGid();
            Optional<ShortLinkCountQueryRespDTO> first = listResult.getData().stream()
                    .filter(item -> Objects.equals(item.getGid(), gid))
                    .findFirst();
            first.ifPresent(item -> each.setShortLinkCount(first.get().getShortLinkCount()));
        });

        return shortlinkGroupRespDTOList;
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setName(requestParam.getName());
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortGroupReqDTO> requestParam) {
        requestParam.forEach(item -> {
            GroupDO groupDO = GroupDO.builder()
                    .gid(item.getGid())
                   .sortOrder(item.getSortOrder())
                   .build();
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getGid, item.getGid())
                    .eq(GroupDO::getDelFlag, 0);
            baseMapper.update(groupDO, updateWrapper);
        });
    }

    private boolean hasGid(String userName,String gid){
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class).eq(GroupDO::getGid, gid)
                .eq(GroupDO::getName, UserContext.getUsername())
                .eq(GroupDO::getUsername, Optional.ofNullable(userName).orElse(UserContext.getUsername()));
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag != null;
    }
}
