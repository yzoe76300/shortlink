package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.admin.dao.entity.UserDO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;

/*
* User用户接口层
* */
public interface IUserService extends IService<UserDO> {

    UserRespDTO getUserByUsername(String username);
}
