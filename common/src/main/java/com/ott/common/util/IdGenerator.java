package com.ott.common.util;

import io.hypersistence.tsid.TSID;

public class IdGenerator {
    public static Long generate() {
        return TSID.Factory
                .getTsid()
                .toLong();
    }
}

