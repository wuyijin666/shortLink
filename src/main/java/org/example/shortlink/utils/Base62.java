package org.example.shortlink.utils;

import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.RandomAccess;

@Component
public class Base62 {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Random RANDOM = new Random();
    private static String shuffledAlphabet;

    private Base62() {
        // 私有构造函数
        shuffledAlphabet = shuffleString();
    }
    public String generateShortUrl(Long id) { return base62Encode(id);}
    /**
     * 将数字编码为 Base62 字符串
     */
    private String base62Encode(Long decimal) {
        StringBuilder sb = new StringBuilder();
        while (decimal > 0) {
            int remainder = (int) (decimal % 62);
            sb.append(shuffledAlphabet.charAt(remainder));
            decimal /= 62;
        }
        return sb.reverse().toString();
    }

    // 打乱字符串的字符顺序
    private String shuffleString() {
        char[] chars = Base62.ALPHABET.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
