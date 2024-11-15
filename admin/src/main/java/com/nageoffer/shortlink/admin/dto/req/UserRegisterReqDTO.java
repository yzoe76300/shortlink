package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;

/*
* 用戶注冊請求DTO參數
* */
@Data
public class UserRegisterReqDTO {
    /**
     * @description t_user
     * @author BEJSON
     * @date 2024-11-11
     *

    private static final long serialVersionUID = 1L;


    /**
     * username
     */
    private String username;

    /**
     * password
     */
    private String password;

    /**
     * real_name
     */
    private String realName;

    /**
     * phone
     */
    private String phone;

    /**
     * email
     */
    private String email;
}
