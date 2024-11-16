package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant;
import com.nageoffer.shortlink.admin.common.convension.exception.ClientException;
import com.nageoffer.shortlink.admin.dao.entity.UserDO;
import com.nageoffer.shortlink.admin.dao.mapper.UserMapper;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN;
import static com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements IUserService {
    // 因爲交給spring管理所以多綫程狀態下注入的是同一個bean（單例bean）
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    
    @Override
    public UserRespDTO getUserByUsername(String username){
    /*
    UserDO::getUsername
    this.name = username;
    所以可以在不创建UserDO对象，直接使用UserDO::getUsername作为条件查询
    * */
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        return result;
    }

    @Override
    public Boolean hasUsername(String username) {
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if (this.hasUsername(requestParam.getUsername())) {
            throw new ClientException(USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(RedisCacheConstant.LOCK_USER_REGISTER_KEY +
                                            requestParam.getUsername());
        try {
            if (lock.tryLock()) {
                UserDO userDO = new UserDO();
                BeanUtils.copyProperties(requestParam, userDO);
                int insert = baseMapper.insert(userDO);
                if(insert < 1){
                    throw new ClientException(USER_NULL);
                }
                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
                return;
            }
            throw new ClientException(USER_NAME_EXIST);
        } finally {
            lock.unlock();
        }


    }

    /**
     * 更新用户信息
     * @param updateParam
     */
    @Override
    public void update(UserUpdateReqDTO updateParam) {
        LambdaQueryWrapper<UserDO> updateWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, updateParam.getUsername());
        baseMapper.update(BeanUtil.toBean(updateParam, UserDO.class),updateWrapper);
    }

    /*
    * 注册并返回uuid作为响应
    * */
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        UserDO userDO = baseMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getPassword, requestParam.getPassword())
                .eq(UserDO::getDelFlag, 0));
        if (userDO == null) {
            throw new ClientException(USER_NULL);
        }
        Boolean hasLogin = stringRedisTemplate.hasKey(USER_LOGIN + requestParam.getUsername());
        if (hasLogin != null && hasLogin){
            throw new ClientException(USER_EXIST);
        }
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put(USER_LOGIN + requestParam.getUsername(), uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire(USER_LOGIN + requestParam.getUsername(), 24L, TimeUnit.HOURS);
        return new UserLoginRespDTO(uuid);
    }


    /*
    * 检查用户是否登录
    * */
    @Override
    public boolean checkLogin(String username, String uuid) {
        return stringRedisTemplate.opsForHash().get(USER_LOGIN + username, uuid) != null;
    }

    @Override
    public void logout(String username, String uuid) {
        if (checkLogin(username, uuid)){
            stringRedisTemplate.delete(USER_LOGIN + username);
        }
        throw new ClientException("用户未登录");
    }
}
