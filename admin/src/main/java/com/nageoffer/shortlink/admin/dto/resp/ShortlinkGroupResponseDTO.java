package com.nageoffer.shortlink.admin.dto.resp;

import lombok.Data;
/*
* 短链接分组返回实体对象
* */
@Data
public class ShortlinkGroupResponseDTO {
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
}
