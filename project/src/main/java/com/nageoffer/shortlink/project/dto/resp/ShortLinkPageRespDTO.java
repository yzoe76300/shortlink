package com.nageoffer.shortlink.project.dto.resp;

import cn.hutool.db.Page;
import lombok.Data;

import java.util.Date;

/*
* 短链接分页返回DTO
* */

@Data
public class ShortLinkPageRespDTO extends Page {
    /**
     * id
     */
    private Long id;

    /**
     * domain
     */
    private String domain;

    /**
     * short_uri
     */
    private String shortUri;

    /**
     * full_short_url
     */
    private String fullShortUrl;

    /**
     * origin_url
     */
    private String originUrl;

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
    private Date validDate;

    /**
     * describe
     */
    private String describe;

    /*
    *  网站标识
    * */
    private String favicon;
}
