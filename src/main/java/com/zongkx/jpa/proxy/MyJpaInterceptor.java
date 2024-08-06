package com.zongkx.jpa.proxy;

import lombok.RequiredArgsConstructor;
import org.hibernate.resource.jdbc.spi.StatementInspector;

@RequiredArgsConstructor
public class MyJpaInterceptor implements StatementInspector {
    @Override
    public String inspect(String s) {

        return s;
    }

    private void exec(String table) {
    }
}