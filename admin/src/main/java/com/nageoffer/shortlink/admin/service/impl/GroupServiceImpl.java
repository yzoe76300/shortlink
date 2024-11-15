package com.nageoffer.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.service.IGroupService;
import com.nageoffer.shortlink.admin.toolkit.RandomGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短链接分组接口业务实现层
 */
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl <GroupMapper, GroupDO> implements IGroupService {

    @Override
    public void saveGroup(String groupName) {
        // 检查是否存在同名gid
        String gid;
        do {
            gid = RandomGenerator.generateRandomString();
        } while(hasGid(gid));
        // 不存在则生成新的gid
        GroupDO groupDO = GroupDO.builder()
                .gid(RandomGenerator.generateRandomString())
                .name(groupName)
                .build();
        baseMapper.insert(groupDO);
    }
    private boolean hasGid(String gid){
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class).eq(GroupDO::getGid, gid)
                // TODO 设置用户名
                .eq(GroupDO::getName, null);
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag != null;
    }
}
