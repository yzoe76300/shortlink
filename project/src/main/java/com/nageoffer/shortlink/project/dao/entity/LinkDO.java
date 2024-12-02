package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nageoffer.shortlink.project.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description t_link 短链接实体类
 * @author BEJSON
 * @date 2024-11-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_link")
public class LinkDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
     * click_num
     */
    private Integer clickNum;

    /**
     * gid
     */
    private String gid;
    /**
     * gid
     */
    private String favicon;
    /**
     * enable_status
     */
    private Integer enableStatus;

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
    @TableField("`describe`")
    private String describe;
    /*
    *  历史PV
    * */
    private Integer totalPv;
    /*
     *  历史UV
     * */
    private Integer totalUv;
    /*
     *  历史UIP
     * */
    private Integer totalUip;

    /**
     * 今日PV
     */
    @TableField(exist = false)
    private Integer todayPv;

    /**
     * 今日UV
     */
    @TableField(exist = false)
    private Integer todayUv;

    /**
     * 今日UIP
     */
    @TableField(exist = false)
    private Integer todayUip;
}
