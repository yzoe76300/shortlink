package com.nageoffer.shortlink.project.common.constant;

/*
* RedisKey常量类
**/
public class RedisKeyConstant {
    /*
    *  短链接跳转前缀key
    * */
    public static final String GOTO_SHORT_LINK_KEY = "short-link:goto_%s";

    /*
     *  短链接跳转锁前缀key
     * */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link:lock:goto_%s";

    /**
     * 短链接修改分组 ID 锁前缀 Key
     */
    public static final String LOCK_GID_UPDATE_KEY = "short-link:lock:update-gid:%s";

    /*
     *  短连链接跳转前缀key
     * */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link:is-null:goto_%s";

    /**
     * 短链接监控消息保存队列 Topic 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_TOPIC_KEY = "short-link:stats-stream";


    /**
     * 短链接延迟队列消费统计 Key
     */
    public static final String DELAY_QUEUE_STATS_KEY = "short-link:delay-queue:stats";

    /**
     * 短链接统计判断是否新用户缓存标识
     */
    public static final String SHORT_LINK_STATS_UV_KEY = "short-link:stats:uv:";

    /**
     * 短链接统计判断是否新 IP 缓存标识
     */
    public static final String SHORT_LINK_STATS_UIP_KEY = "short-link:stats:uip:";

    /**
     * 短链接监控消息保存队列 Group 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_GROUP_KEY = "short-link:stats-stream:only-group";
    /**
     * 短链接监控消息保存队列幂等性校验标识
     */
    public static final String IDEMPOTENT_KEY_PREFIX = "short-link:idempotent:";

}
