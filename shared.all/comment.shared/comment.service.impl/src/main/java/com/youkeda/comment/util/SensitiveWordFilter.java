package com.youkeda.comment.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词过滤器（前缀树 Trie 实现）
 *
 * 为什么要单独写一个类，而不是在 Service 里循环 contains：
 *   假设词库有 1000 个词，每条评论长度 100 字。
 *   逐个 contains 的时间复杂度是 O(词数 × 评论长度) = 10 万次字符比较；
 *   前缀树的复杂度是 O(评论长度 × 最长词长度)，通常只有几百次。
 *
 * 前缀树的原理（举例词库：["垃圾", "垃圾广告"]）：
 *
 *          root
 *           │ 垃
 *          [垃]
 *           │ 圾
 *          [圾] ← endOfWord = true（"垃圾"在这里结束）
 *           │ 广
 *          [广]
 *           │ 告
 *          [告] ← endOfWord = true（"垃圾广告"在这里结束）
 *
 * 匹配时从每个字符位置出发，沿着树往下走，走不动就换下一个起点。
 * 走到 endOfWord 就是命中。
 *
 * 为什么用 HashMap 存子节点而不是数组：
 *   中文有几万个字符，用数组（256 或 65536）会浪费大量内存；
 *   HashMap 只存实际出现的字符，词库再大也不会爆。
 */
@Component
public class SensitiveWordFilter {

    /** 前缀树节点 */
    private static class Node {
        /** 子节点：字符 -> 子节点 */
        private final Map<Character, Node> children = new HashMap<>();
        /** 是否有词在这个节点结束 */
        private boolean endOfWord;
        /** 完整词，命中时直接返回，省得再从路径回溯拼字符串 */
        private String word;
    }

    /** 树根，不存字符 */
    private final Node root = new Node();

    /** 词库大小，只为日志好看 */
    private int wordCount = 0;

    /** 词库文件路径：打包在 jar 的 classpath 根目录 */
    private static final String WORDS_FILE = "sensitive-words.txt";

    /**
     * 应用启动后构建一次前缀树，之后所有请求共用。
     * 用 @PostConstruct 而不是构造函数：等 Spring 把对象装配好再执行。
     *
     * ⚠ 为什么不把词库写在 application.properties 里：
     *   Spring Boot 2.3 的 properties 按 ISO-8859-1 解析，中文词库读进来就是乱码，
     *   过滤器会加载成功但永远匹配不上（这是实际踩过的坑）。
     *   改用独立的 UTF-8 文件读，编码完全可控；词库上千条时配置文件也不至于爆炸。
     */
    @PostConstruct
    public void init() {
        ClassPathResource resource = new ClassPathResource(WORDS_FILE);
        if (!resource.exists()) {
            System.out.println("[敏感词] 未找到词库文件 " + WORDS_FILE + "，过滤器不生效");
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (word.isEmpty() || word.startsWith("#")) {
                    continue;
                }
                insert(word);
                wordCount++;
                if (wordCount <= 3) {
                    // 打印前几个词，方便启动时肉眼确认编码没坏
                    System.out.println("[敏感词] 加载: " + word);
                }
            }
        } catch (IOException e) {
            System.out.println("[敏感词] 词库读取失败: " + e.getMessage());
            return;
        }
        System.out.println("[敏感词] 词库加载完成，共 " + wordCount + " 个词");
    }

    /** 往树里插入一个词 */
    private void insert(String word) {
        Node node = root;
        for (char c : word.toCharArray()) {
            // computeIfAbsent：没有这个字符就新建一个节点
            node = node.children.computeIfAbsent(c, k -> new Node());
        }
        node.endOfWord = true;
        node.word = word;
    }

    /**
     * 找出文本中第一个命中的敏感词
     *
     * @return 命中的词；没命中返回 null
     */
    public String findFirst(String text) {
        if (text == null || text.isEmpty() || wordCount == 0) {
            return null;
        }
        for (int start = 0; start < text.length(); start++) {
            Node node = root;
            for (int i = start; i < text.length(); i++) {
                node = node.children.get(text.charAt(i));
                if (node == null) {
                    break;      // 这条路径断了，换下一个起点
                }
                if (node.endOfWord) {
                    return node.word;
                }
            }
        }
        return null;
    }

    /**
     * 把文本里的敏感词替换成指定字符（比如 *）
     * 有些业务不拒绝发布，而是打码后放行，用这个方法。
     */
    public String replaceAll(String text, char mask) {
        if (text == null || text.isEmpty() || wordCount == 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text);
        for (int start = 0; start < text.length(); start++) {
            Node node = root;
            for (int i = start; i < text.length(); i++) {
                node = node.children.get(text.charAt(i));
                if (node == null) {
                    break;
                }
                if (node.endOfWord) {
                    // 把这段字符全部替换成 mask（注意：StringBuilder 下标和原串一致）
                    for (int k = start; k <= i; k++) {
                        sb.setCharAt(k, mask);
                    }
                    start = i;      // 跳过已替换的部分
                    break;
                }
            }
        }
        return sb.toString();
    }
}
