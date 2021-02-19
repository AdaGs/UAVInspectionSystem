package com.wgx.sgcc.config;

import java.util.HashMap;
import java.util.Map;

public class ValidCodeCache {

    public static Map<String, Object> map = new HashMap<>();
    /**
     * 清理缓存数据
     */
    public static void clearCache(){
        map.clear();
    }
    /**
     * 获取缓存数据
     */
    public static Object getRealLink(String key){
        return map.get(key);
    }
}
