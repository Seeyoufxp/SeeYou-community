package com.seeyou.ai.function;

import java.io.Serializable;

/**
 * Function Calling 专用：空请求对象
 * 注册时长、天气两个 Function 均不需要 LLM 传参（当前用户身份从 UserContext 取），
 * 但 Spring AI Function<I,O> 要求有输入类型，故用此空对象占位。
 * LLM 看到无字段的 schema 会直接调用，不传参数。
 */
public class FunctionEmptyRequest implements Serializable {

    private static final long serialVersionUID = 1L;
}
