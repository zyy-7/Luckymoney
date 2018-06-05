package com.codeboy.qianghongbao.job;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.codeboy.qianghongbao.BuildConfig;
import com.codeboy.qianghongbao.Config;
import com.codeboy.qianghongbao.QiangHongBaoService;
import com.codeboy.qianghongbao.util.AccessibilityHelper;
import com.codeboy.qianghongbao.util.NotifyHelper;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Created 16/1/16 上午12:40.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
public class WechatAccessibilityJob extends BaseAccessibilityJob {

    private static final String TAG = "WechatAccessibilityJob";

    /** 微信的包名*/
    public static final String WECHAT_PACKAGENAME = "com.tencent.mm";

    /** 红包消息的关键字*/
    private static final String HONGBAO_TEXT_KEY = "[微信红包]";

    private static final String BUTTON_CLASS_NAME = "android.widget.Button";


    /** 不能再使用文字匹配的最小版本号 */
    private static final int USE_ID_MIN_VERSION = 700;// 6.3.8 对应code为680,6.3.9对应code为700

    private static final int WINDOW_NONE = 0;
    private static final int WINDOW_LUCKYMONEY_RECEIVEUI = 1;
    private static final int WINDOW_LUCKYMONEY_DETAIL = 2;
    private static final int WINDOW_LAUNCHER = 3;
    private static final int WINDOW_OTHER = -1;

    private int mCurrentWindow = WINDOW_NONE;

    private boolean isReceivingHongbao;
    private PackageInfo mWechatPackageInfo = null;
    private Handler mHandler = null;

    private int index = 0;
    private long receiveTime = 0;
    SharedPreferences sp;


    @Override
    public void onCreateJob(QiangHongBaoService service) {
        super.onCreateJob(service);
        sp = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE);
        updatePackageInfo();
    }

    @Override
    public void onStopJob() {
    }

    @Override
    public String getTargetPackageName() {
        return WECHAT_PACKAGENAME;
    }

    @Override
    public void onReceiveJob(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        //接收到通知栏事件
        if(eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Parcelable data = event.getParcelableData();
            if(data == null || !(data instanceof Notification)) {
                return;
            }
            List<CharSequence> texts = event.getText();
            if(!texts.isEmpty()) {
                String text = String.valueOf(texts.get(0));
                notificationEvent(text, (Notification) data);
            }
        } else if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            openHongBao(event);
        }
        //当跳转到聊天界面的时候会发生该事件
        else if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if(mCurrentWindow != WINDOW_LAUNCHER) { //不在聊天界面或聊天列表，不处理
                return;
            }
            if(isReceivingHongbao) {
                handleChatListHongBao();
            }
        } else if(eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && event.getClassName().equals("android.widget.LinearLayout")
                && sp.getBoolean("detect", false)){

            Timestamp ts = new Timestamp(System.currentTimeMillis());//用于获得当前的 年月日
            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //年月日 格式
            DateFormat fullsdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//年月日 时分秒 格式

            String tsStr = sdf.format(ts);
            String eventText = event.getText().toString();//列表项中的文字
            eventText = eventText.replaceAll("\\[|\\]", "");
            Log.d(TAG, eventText);
            Pattern p = Pattern.compile("(.+?), (\\d+:\\d+:\\d+)");//进行正则匹配，以提取出文字中的 用户名与时间戳，时间戳格式为：时分秒
            Matcher m = p.matcher(eventText);
            if (m.find()) {
                String name = m.group(1);//正则表达式捕获到的用户名
                String stime = m.group(2);//正则表达式捕获到的时间戳
                String takeTime = tsStr + " " + stime;
                try {
                    //对String类型的时间戳进行转换与比较，判断其是否是外挂
                    Date takeTimeDate = fullsdf.parse(takeTime);
                    long timeGap = takeTimeDate.getTime() - receiveTime;
                    int timeLength = sp.getInt("time", 2000);
                    if (timeGap < timeLength){
                        Toast.makeText(getContext(), name+"可能是外挂", Toast.LENGTH_SHORT).show();
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            clickTime();//继续点击下一个列表项
        }

    }



    /** 通知栏事件的处理*/
    private void notificationEvent(String ticker, Notification nf) {
        String text = ticker;
        int index = text.indexOf(":");
        if(index != -1) {
            text = text.substring(index + 1);
        }
        text = text.trim();
        if(text.contains(HONGBAO_TEXT_KEY)) { //通过判断通知消息中是否包含关键字判断是否是红包消息
            receiveTime = System.currentTimeMillis();
            newHongBaoNotification(nf);
        }
    }

    /** 打开通知栏消息*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void newHongBaoNotification(Notification notification) {
        isReceivingHongbao = true;
        PendingIntent pendingIntent = notification.contentIntent;
        boolean lock = NotifyHelper.isLockScreen(getContext());

        if(!lock) {
            NotifyHelper.send(pendingIntent);//pendingIntent打开该条通知栏消息，跳转到聊天界面
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openHongBao(AccessibilityEvent event) {
        if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LUCKYMONEY_RECEIVEUI;
            //表示在拆红包界面，于是进行组件的查找与点击
            handleLuckyMoneyReceive();
        } else if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LUCKYMONEY_DETAIL;
            Log.w(TAG, "detail");
            //拆完红包后看详细的纪录界面
            clickTime();

        } else if("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LAUNCHER;
            //在聊天界面,去点中红包
            handleChatListHongBao();
        } else {
            mCurrentWindow = WINDOW_OTHER;
        }
    }

    private boolean clickTime() {
        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
			Log.w(TAG, "rootWindow为空");
            return true;
		}

		/*详情页界面结构为：
		* rootView ---> FrameLayout ---> ListView ---> 具体的列表项*/

        AccessibilityNodeInfo listNode = null;
		//根据对界面的分析，发现除标题栏以外的内容都在android.widget.FrameLayout这个子布局之中
        AccessibilityNodeInfo frameNode = AccessibilityHelper.findNodeInfosByClassName(nodeInfo, "android.widget.FrameLayout");

        //android.widget.ListView是抢红包情况的列表项所在的父布局
        if (frameNode != null)
            listNode = AccessibilityHelper.findNodeInfosByClassName(frameNode, "android.widget.ListView");

        if (listNode != null && index < listNode.getChildCount()){
            AccessibilityNodeInfo node = listNode.getChild(index);
            AccessibilityHelper.performClick(node);
            index++;
        }
        else {
            index = 0;
        }
        return false;
    }

    /**
     * 点击聊天里的红包后，进行组件查找与点击
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleLuckyMoneyReceive() {
        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
            return;
        }

        AccessibilityNodeInfo targetNode = null;

        int event = getConfig().getWechatAfterOpenHongBaoEvent();
        int wechatVersion = getWechatVersion();
        if(event == Config.WX_AFTER_OPEN_HONGBAO) { //拆红包
            if (wechatVersion < USE_ID_MIN_VERSION) {
                targetNode = AccessibilityHelper.findNodeInfosByText(nodeInfo, "拆红包");
            } else {
                String buttonId = "com.tencent.mm:id/b43";

                if(wechatVersion == 700) {
                    buttonId = "com.tencent.mm:id/b2c";
                }
                if(buttonId != null) {
                    targetNode = AccessibilityHelper.findNodeInfosById(nodeInfo, buttonId);
                }

                if(targetNode == null) { //通过组件查找
                    targetNode = AccessibilityHelper.findNodeInfosByClassName(nodeInfo, BUTTON_CLASS_NAME);
                }
            }
        }

        if(targetNode != null) {
            AccessibilityHelper.performClick(targetNode);
        }
    }

    /**
     * 收到聊天里的红包
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleChatListHongBao() {

        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");

        if(list != null && list.isEmpty()) {
            // 从消息列表查找红包
            AccessibilityNodeInfo node = AccessibilityHelper.findNodeInfosByText(nodeInfo, "[微信红包]");
            if(node != null) {
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "-->微信红包:" + node);
                }
                isReceivingHongbao = true;
                AccessibilityHelper.performClick(nodeInfo);
            }
        } else if(list != null) {
            if (isReceivingHongbao){
                //最新的红包领起
                receiveTime = System.currentTimeMillis();
                AccessibilityNodeInfo node = list.get(list.size() - 1);
                AccessibilityHelper.performClick(node);
                isReceivingHongbao = false;
            }
        }
    }

    private Handler getHandler() {
        if(mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /** 获取微信的版本*/
    private int getWechatVersion() {
        if(mWechatPackageInfo == null) {
            return 0;
        }
        return mWechatPackageInfo.versionCode;
    }

    /** 更新微信包信息*/
    private void updatePackageInfo() {
        try {
            mWechatPackageInfo = getContext().getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
