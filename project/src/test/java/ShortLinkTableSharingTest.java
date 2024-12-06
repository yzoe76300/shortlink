public class ShortLinkTableSharingTest {
    public static final String SQL1 = "ALTER TABLE t_link_%d ADD COLUMN del_time BIGINT(20) NOT NULL DEFAULT 0;";
    public static final String SQL2 = "CREATE INDEX idx_full_short_url_del_time ON t_link_%d (full_short_url, del_time);";
    public static void main(String[] args) {
        for (int i = 0; i < 16; i++) {
            System.out.printf((SQL1) + "%n", i);
            System.out.printf((SQL2) + "%n", i);
        }
    }
}
