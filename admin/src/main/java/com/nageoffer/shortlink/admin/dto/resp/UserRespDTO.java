package com.nageoffer.shortlink.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nageoffer.shortlink.admin.common.serialize.PhoneDesensitizationSerializer;
import lombok.Data;

import java.util.Date;

/*
* 用户返回参数响应
* */
@Data
public class UserRespDTO {
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
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
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
