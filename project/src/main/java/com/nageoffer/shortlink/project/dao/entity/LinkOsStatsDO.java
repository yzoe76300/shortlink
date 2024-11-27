package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/*
*  操作系统访问实体
* */
@TableName("t_link_local_stats")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkOsStatsDO {
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

    /*
    *  operating system
    * */
    private String os;
}
