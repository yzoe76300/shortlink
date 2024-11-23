public class ShortLinkTableSharingTest {
    public static final String SQL = "CREATE TABLE `t_link_goto_%d` (\n" +
            "  `id` bigint unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  `gid` varchar(32) DEFAULT NULL,\n" +
            "  `full_short_url` varchar(128) DEFAULT NULL,\n" +
            "  UNIQUE KEY `id` (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;";
    public static void main(String[] args) {
        for (int i = 0; i < 16; i++) {
            System.out.printf((SQL) + "%n", i);
        }
    }
}
