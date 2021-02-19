package com.wgx.sgcc.util;

import com.wgx.sgcc.config.ValidCodeCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduleTask {

    @Scheduled(cron = "0 0 1 * * ? ")
    public void clearMap(){
        ValidCodeCache.clearCache();

    }
}
