package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nageoffer.shortlink.project.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/*
*  短链接基础访问实体类
*  */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName("t_link_access_stats")
public class LinkAccessStatsDO extends BaseDO {
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
     * page view
     */
    private Integer pv;

    /**
     * user view
     */
    private Integer uv;


    /**
     * user unique ip
     */
    private Integer uip;

    /**
     * hour
     */
    private Integer hour;

    /**
     * weekday
     */
    private Integer weekday;
}
