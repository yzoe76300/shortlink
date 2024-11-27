package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkBrowserStatsDO;
import org.apache.ibatis.annotations.Insert;

/*
*  浏览器统计访问持久层接口
* */

public interface LinkBrowserStatsMapper extends BaseMapper<LinkBrowserStatsDO> {
    @Insert("INSERT INTO t_link_browser_stats ( full_short_url, gid, date, cnt, browser, create_time, update_time, del_flag )" +
            "VALUES(#{fullShortUrl}, #{gid}, #{date}, #{cnt}, #{browser}, NOW(), NOW(), 0 )" +
            "ON DUPLICATE KEY UPDATE  cnt = cnt + #{cnt};")
    void shortLinkBrowserStats(LinkBrowserStatsDO linkBrowserStatsDO);
}
