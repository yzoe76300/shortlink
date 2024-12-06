package com.nageoffer.shortlink.admin.common.constant;
/*
* 短链接后管redis缓存常量
* */
public class RedisCacheConstant {

    public static final String LOCK_GROUP_CREATE_KEY = "short-link:lock_group-create:";
    /*
    *  用户分布式锁注册
    * */
    public static final String LOCK_USER_REGISTER_KEY = "short-link:lock_user-register:";
    public static final String USER_LOGIN = "login:";
}
