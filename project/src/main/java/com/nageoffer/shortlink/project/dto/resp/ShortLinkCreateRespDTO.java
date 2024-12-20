package com.nageoffer.shortlink.project.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建响应对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkCreateRespDTO {
    /*
     *  分组信息
     * */
    private String gid;
    /**
     * origin_url
     */
    private String originUrl;

    /**
     * short_url
     */
    private String fullShortUrl;
}
