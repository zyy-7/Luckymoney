package com.codeboy.qianghongbao.job;

import android.view.accessibility.AccessibilityEvent;

import com.codeboy.qianghongbao.QiangHongBaoService;

public interface AccessibilityJob {
    String getTargetPackageName();
    void onCreateJob(QiangHongBaoService service);
    void onReceiveJob(AccessibilityEvent event);
    void onStopJob();
}
