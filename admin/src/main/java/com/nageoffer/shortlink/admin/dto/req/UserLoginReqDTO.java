package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;
/*
*  用户登录请求参数
* */
@Data
public class UserLoginReqDTO {
    /**
     * username
     */
    private String username;
    /**
     * password
     */
    private String password;
}
