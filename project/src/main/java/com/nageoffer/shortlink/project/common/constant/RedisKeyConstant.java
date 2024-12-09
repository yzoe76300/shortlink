package com.nageoffer.shortlink.project.common.constant;

/*
* RedisKey常量类
**/
public class RedisKeyConstant {
    /*
    *  短链接跳转前缀key
    * */
    public static final String GOTO_SHORT_LINK_KEY = "short-link_goto_%s";

    /*
     *  短链接跳转锁前缀key
     * */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link_lock_goto_%s";

    /*
     *  短连链接跳转前缀key
     * */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link_goto_%s";

    /**
     * 短链接监控消息保存队列 Topic 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_TOPIC_KEY = "short-link:stats-stream";

    /**
     * 短链接监控消息保存队列 Group 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_GROUP_KEY = "short-link:stats-stream:only-group";
    /**
     * 短链接监控消息保存队列幂等性校验标识
     */
    public static final String IDEMPOTENT_KEY_PREFIX = "short-link:idempotent:";

}
