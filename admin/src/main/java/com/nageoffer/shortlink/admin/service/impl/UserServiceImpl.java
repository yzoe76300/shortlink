package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant;
import com.nageoffer.shortlink.admin.common.convension.exception.ClientException;
import com.nageoffer.shortlink.admin.dao.entity.UserDO;
import com.nageoffer.shortlink.admin.dao.mapper.UserMapper;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.IGroupService;
import com.nageoffer.shortlink.admin.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
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
    private final IGroupService groupService ;
    
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
        if (!lock.tryLock()){
            throw new ClientException(USER_NAME_EXIST);
        }
            UserDO userDO = new UserDO();
            BeanUtils.copyProperties(requestParam, userDO);
            try {
                int insert = baseMapper.insert(userDO);
                if(insert < 1){
                    throw new ClientException(USER_NULL);
                }
                // 给每个用户创建一个默认的组
                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
                groupService.saveGroup(requestParam.getUsername(), "default");
            } catch (DuplicateKeyException e) {
                throw new ClientException(USER_EXIST);
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
        if (!Objects.equals(updateParam.getUsername(), UserContext.getUsername())){
            throw new ClientException("当前用户登录修改请求异常");
        }
        LambdaQueryWrapper<UserDO> updateWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, updateParam.getUsername());
        baseMapper.update(BeanUtil.toBean(updateParam, UserDO.class),updateWrapper);
    }

    /*
    * 注册并返回uuid作为响应
    * */
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getPassword, requestParam.getPassword())
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(USER_NULL);
        }
        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(USER_LOGIN + requestParam.getUsername());
        // 存在则刷新有效期并返回token
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            stringRedisTemplate.expire(USER_LOGIN + requestParam.getUsername(), 30L, TimeUnit.MINUTES);
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登录错误"));
            return new UserLoginRespDTO(token);
        }
        // 不存在则生成uuid并存入redis
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put(USER_LOGIN + requestParam.getUsername(), uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire(USER_LOGIN + requestParam.getUsername(), 30L, TimeUnit.MINUTES);
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
