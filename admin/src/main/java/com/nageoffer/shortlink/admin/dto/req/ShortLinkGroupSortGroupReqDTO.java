package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;

/*
* 短链接分组排序
* */
@Data
public class ShortLinkGroupSortGroupReqDTO {
    /*
    * short link group id
    * */
    private String gid;
    /*
    * shor link group sort order
    * */
    private Integer sortOrder;
}
