package com.nageoffer.shortlink.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 持久化对象实体
 */
@Data
@TableName("t_user")
public class UserDO {
    /**
     * @description t_user
     * @author BEJSON
     * @date 2024-11-11
     *

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

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

    /**
     * deletion_time
     */
    private Long deletionTime;

    /**
     * create_time
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * del_flag
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;

    /**
     * update_time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
