public class ShortLinkTableSharingTest {
    public static final String SQL = "CREATE TABLE `t_link_%d` (\n" +
            "  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
            "  `domain` varchar(128) DEFAULT NULL,\n" +
            "  `short_uri` varchar(8) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,\n" +
            "  `full_short_url` varchar(128) DEFAULT NULL,\n" +
            "  `origin_url` varchar(1024) DEFAULT NULL,\n" +
            "  `click_num` int DEFAULT '0',\n" +
            "  `gid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'default',\n" +
            "  `favicon` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,\n" +
            "  `enable_status` tinyint(1) DEFAULT NULL,\n" +
            "  `created_type` tinyint(1) DEFAULT NULL,\n" +
            "  `valid_date_type` tinyint(1) DEFAULT NULL,\n" +
            "  `valid_date` datetime DEFAULT NULL,\n" +
            "  `describe` varchar(1024) DEFAULT NULL,\n" +
            "  `create_time` datetime DEFAULT NULL,\n" +
            "  `update_time` datetime DEFAULT NULL,\n" +
            "  `del_flag` tinyint(1) DEFAULT NULL,\n" +
            "  UNIQUE KEY `id` (`id`),\n" +
            "  UNIQUE KEY `idx_unique_full_short_url` (`full_short_url`) USING BTREE\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;";
    public static void main(String[] args) {
        for (int i = 0; i < 16; i++) {
            System.out.printf((SQL) + "%n", i);
        }
    }
}
