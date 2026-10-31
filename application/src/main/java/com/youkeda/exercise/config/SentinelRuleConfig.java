package com.youkeda.exercise.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 限流规则（代码方式配置，不依赖 Dashboard）
 *
 * 为什么用代码配：
 *   Dashboard 只是规则的"可视化编辑器"，规则最终都存到 FlowRuleManager 里。
 *   直接调 loadRules 效果完全一样，还省掉下载安装 Dashboard 的步骤。
 *
 * 三个关键点：
 *   ① resource 名字必须和 @SentinelResource(value = "...") 完全一致，对不上规则就是废的
 *   ② grade = QPS 表示按"每秒请求数"限，另一种是"并发线程数"
 *   ③ count = 2 表示每秒最多放行 2 个，第 3 个开始被拒绝
 */
@Configuration
public class SentinelRuleConfig {

    /** 资源名，必须和 CommentController 里 @SentinelResource 的 value 一模一样 */
    private static final String RESOURCE_FIND_BY_REF_ID = "findByRefId";

    /** 每秒放行的请求数上限 */
    private static final double QPS_LIMIT = 2;

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule rule = new FlowRule();
        rule.setResource(RESOURCE_FIND_BY_REF_ID);                      // 限哪个资源
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);                     // 按 QPS 限流
        rule.setCount(QPS_LIMIT);                                       // 阈值：2 次/秒
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT); // 直接拒绝（还有排队等待/预热两种）
        rules.add(rule);

        FlowRuleManager.loadRules(rules);                               // 规则装进内存
        System.out.println("[Sentinel] 限流规则已加载: " + RESOURCE_FIND_BY_REF_ID + " QPS = " + QPS_LIMIT);
    }
}
