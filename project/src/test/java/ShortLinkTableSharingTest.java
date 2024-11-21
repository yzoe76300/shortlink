public class ShortLinkTableSharingTest {
    public static final String SQL = "CREATE TABLE `t_group_%d` (\n" +
            "  `id` bigint unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  `gid` varchar(32) DEFAULT NULL,\n" +
            "  `name` varchar(64) DEFAULT NULL,\n" +
            "  `username` varchar(256) DEFAULT NULL,\n" +
            "  `sort_order` int DEFAULT NULL,\n" +
            "  `create_time` datetime DEFAULT NULL,\n" +
            "  `update_time` datetime DEFAULT NULL,\n" +
            "  `del_flag` tinyint(1) DEFAULT NULL,\n" +
            "  UNIQUE KEY `id` (`id`),\n" +
            "  UNIQUE KEY `idx_unique_gid_username` (`gid`,`username`) USING BTREE\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;";
    public static void main(String[] args) {
        for (int i = 0; i < 16; i++) {
            System.out.printf((SQL) + "%n", i);
        }
    }
}
