package com.nageoffer.shortlink.project.dto.req;

import lombok.Data;

import java.util.Date;

/*
* 短链接创建请求实体类
* */
@Data
public class ShortLinkCreateReqDTO {

    /**
     * domain
     */
    private String domain;
    /**
     * origin_url
     */
    private String originUrl;

    /**
     * gid
     */
    private String gid;

    /**
     * created_type
     */
    private Integer createdType;

    /**
     * valid_date_type
     */
    private Integer validDateType;

    /**
     * valid_date
     */
    private Date validDate;

    /**
     * describe
     */
    private String describe;

}
