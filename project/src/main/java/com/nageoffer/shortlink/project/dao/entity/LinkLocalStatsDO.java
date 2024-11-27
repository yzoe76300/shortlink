package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/*
*  地区统计访问实体
* */
@TableName("t_link_local_stats")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkLocalStatsDO {
    /**
     * id
     */
    private Long id;

    /**
     * full_short_url
     */
    private String fullShortUrl;

    /**
     * gid
     */
    private String gid;

    /**
     * date
     */
    private Date date;

    /**
     * count
     */
    private Integer cnt;

    /**
     * province
     */
    private String province;

    /**
     * city
     */
    private String city;

    /**
     * adcode
     */
    private String adcode;

    /**
     * country
     */
    private String country;

    /**
     * create_time
     */
    private Date createTime;

    /**
     * update_time
     */
    private Date updateTime;

    /**
     * del_flag
     */
    private Integer delFlag;
}
