package com.nageoffer.shortlink.admin.remote.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/*
* 短链接创建请求实体类
* */
@Data
public class ShortLinkUpdateReqDTO {

    /**
     * origin_url
     */
    private String originUrl;

    /*
    *  full_short_url
    * */
    private String fullShortUrl;

    /**
     * gid
     */
    private String gid;

    /**
     * valid_date_type
     */
    private Integer validDateType;

    /**
     * valid_date
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;

    /**
     * describe
     */
    private String describe;

}
