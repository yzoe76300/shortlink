package com.nageoffer.shortlink.admin.dao.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
/*
* 短链接分组实体
* */
@Data
@TableName("t_group")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDO {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    /**
     * id
     */
    private Long id;

    /**
     * gid
     */
    private String gid;

    /**
     * name
     */
    private String name;

    /**
     * username
     */
    private String username;
    /**
     * 分组排序
     */
    private Integer sortOrder;

    /**
     * create_time
     */
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
