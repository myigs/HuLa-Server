package com.hula.ai.llm.openai.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * 描述： 日志
 *
 */
@Slf4j
public class OpenAILogger implements HttpLoggingInterceptor.Logger {
    @Override
    public void log(String message) {
        log.info("OkHttp-------->:{}", message);
    }
}
