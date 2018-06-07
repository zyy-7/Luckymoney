package com.codeboy.qianghongbao.job;

import android.view.accessibility.AccessibilityEvent;

import com.codeboy.qianghongbao.LuckymoneyService;

public interface AccessibilityJob {
    String getTargetPackageName();
    void onCreateJob(LuckymoneyService service);
    void onReceiveJob(AccessibilityEvent event);
    void onStopJob();
}
