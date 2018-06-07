package com.codeboy.qianghongbao.job;

import android.content.Context;

import com.codeboy.qianghongbao.Config;
import com.codeboy.qianghongbao.LuckymoneyService;

/**
 * <p>Created 16/1/16 上午12:38.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
public abstract class BaseAccessibilityJob implements AccessibilityJob {

    private LuckymoneyService service;

    @Override
    public void onCreateJob(LuckymoneyService service) {
        this.service = service;
    }

    public Context getContext() {
        return service.getApplicationContext();
    }

    public Config getConfig() {
        return service.getConfig();
    }

    public LuckymoneyService getService() {
        return service;
    }
}
