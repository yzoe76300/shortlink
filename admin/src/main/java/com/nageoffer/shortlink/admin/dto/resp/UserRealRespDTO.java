package com.nageoffer.shortlink.admin.dto.resp;

import lombok.Data;

import java.util.Date;

/*
 * 用户返回参数响应
 * */
@Data
public class UserRealRespDTO {
    /**
     * id
     */
    private Long id;

    /**
     * username
     */
    private String username;

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

    /**
     * deletion_time
     */
    private Long deletionTime;

    /**
     * create_time
     */
    private Date createTime;

    /**
     * del_flag
     */
    private Integer delFlag;

    /**
     * update_time
     */
    private Date updateTime;
}
