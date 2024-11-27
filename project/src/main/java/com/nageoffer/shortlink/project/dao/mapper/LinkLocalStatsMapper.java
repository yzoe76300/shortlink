package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkLocalStatsDO;
import org.apache.ibatis.annotations.Insert;

public interface LinkLocalStatsMapper extends BaseMapper<LinkLocalStatsDO> {
    /*
    *  记录地区访问量
    * */
    @Insert("INSERT INTO t_link_local_stats ( full_short_url, gid, date, cnt, province, city, adcode, country, create_time, update_time, del_flag )" +
            "VALUES(#{fullShortUrl},#{gid},#{date}, #{cnt}, #{province}, #{city} , #{adcode}, #{country}, NOW(), NOW(), 0 )" +
            "ON DUPLICATE KEY UPDATE cnt = cnt + #{cnt};")
    void shortLinkLocalStats(LinkLocalStatsDO linkLocalStatsDO);
}
