package com.nageoffer.shortlink.project.common.database;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/*
*数据库持久层对象基础属性
* */
@Data
public class BaseDO {
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * update_time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * del_flag
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
