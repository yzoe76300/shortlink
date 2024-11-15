package com.nageoffer.shortlink.admin.toolkit;

import java.util.Random;

/**
 * 分组ID生成器
 */
public class RandomGenerator {
    public static String generateRandomString(){
        return generateRandomString(6);
    }

    /**
     * 生成包含数字和英文字母的随机字符串。
     *
     * @param length 字符串的长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be a positive integer.");
        }

        // 定义一个字符串，包含所有可能的字符（字母和数字）
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder randomStringBuilder = new StringBuilder(length);

        // 循环length次，每次随机选择一个字符
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            randomStringBuilder.append(characters.charAt(randomIndex));
        }

        return randomStringBuilder.toString();
    }
}
