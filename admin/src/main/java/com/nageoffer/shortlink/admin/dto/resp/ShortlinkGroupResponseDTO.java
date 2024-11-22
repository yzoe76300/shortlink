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
     * 分组排序
     */
    private Integer sortOrder;

    /*
    *  分组下短链接数量
    * */

    private Integer shortLinkCount;
}
