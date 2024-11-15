package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.admin.dao.entity.UserDO;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;

/*
* User用户接口层
* */
public interface IUserService extends IService<UserDO> {
    /*
    * 根据用户名获取用户信息
     */
    UserRespDTO getUserByUsername(String username);

    /*
    * 查看用户是否可用
    * */
    Boolean hasUsername(String username);

    void register(UserRegisterReqDTO requestParam);


    void update(UserUpdateReqDTO updateParam);

    UserLoginRespDTO login(UserLoginReqDTO requestParam);
    /*
    * 检查用户是否登录
    * */
    boolean checkLogin(String username, String uuid);

    void logout(String username, String uuid);
}
