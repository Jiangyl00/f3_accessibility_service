package slayer.accessibility.service.flutter_accessibility_service;

import static slayer.accessibility.service.flutter_accessibility_service.Constants.*;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.flutter.FlutterInjector;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.FlutterEngineGroup;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.JSONUtil;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * FlutterAccessibilityServicePlugin
 */
public class FlutterAccessibilityServicePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener, EventChannel.StreamHandler  {


    private static final String CHANNEL_TAG = "x-slayer/accessibility_channel";
    private static final String EVENT_TAG = "x-slayer/accessibility_event";
    public static final String CACHED_TAG = "cashedAccessibilityEngine";


    private MethodChannel channel;
    private AccessibilityReceiver accessibilityReceiver;
    private EventChannel eventChannel;
    private Context context;
    private Activity mActivity;
    private boolean supportOverlay = false;

    private Result pendingResult;
    final int REQUEST_CODE_FOR_ACCESSIBILITY = 167;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL_TAG);
        channel.setMethodCallHandler(this);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENT_TAG);
        eventChannel.setStreamHandler(this);
    }

    private BroadcastReceiver actionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<Integer> actions = intent.getIntegerArrayListExtra("actions");
            pendingResult.success(actions);
        }
    };
    private BroadcastReceiver actionsEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("监听到返回事件元素 actions_node ");
            ArrayList<Parcelable> actions = intent.getParcelableArrayListExtra("actions_node");

            if(actions.size()>0){
                //String json = convertToJson(actions);
                Gson gson = new Gson();

                // 将自定义对象列表转换为 JSON 字符串
                String json = gson.toJson(actions);
                pendingResult.success(json);
            }else{
                pendingResult.success("");
            }

        }
    };
    public static String convertToJson(List<AccessibilityNodeInfo> nodeInfoList) {
        List<AccessibilityNodeInfoData> dataList = new ArrayList<>();

        for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
            // 提取你需要的信息
            String className = nodeInfo.getClassName().toString();
            CharSequence text = nodeInfo.getText();

            // 创建自定义对象并添加到列表中
            AccessibilityNodeInfoData data = new AccessibilityNodeInfoData(className, text != null ? text.toString() : "");
            dataList.add(data);
        }

        // 创建 Gson 实例
        Gson gson = new Gson();

        // 将自定义对象列表转换为 JSON 字符串
        String json = gson.toJson(dataList);

        return json;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        pendingResult = result;
        if (call.method.equals("isAccessibilityPermissionEnabled")) {
            result.success(Utils.isAccessibilitySettingsOn(context));
        } else if (call.method.equals("requestAccessibilityPermission")) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            mActivity.startActivityForResult(intent, REQUEST_CODE_FOR_ACCESSIBILITY);
        } else if (call.method.equals("getSystemActions")) {
            if (Utils.isAccessibilitySettingsOn(context)) {
                IntentFilter filter = new IntentFilter(BROD_SYSTEM_GLOBAL_ACTIONS);
                context.registerReceiver(actionsReceiver, filter);
                Intent intent = new Intent(context, AccessibilityListener.class);
                intent.putExtra(INTENT_SYSTEM_GLOBAL_ACTIONS, true);
                context.startService(intent);
            } else {
                result.error("SDK_INT_ERROR", "Invalid SDK_INT", null);
            }
        }else if(call.method.equals("fvtxt")) {
            //0329 根据nodeId查找Node节点
            IntentFilter filter = new IntentFilter(BROD_EVENT_ITEMS_ACTIONS);
            context.registerReceiver(actionsEventReceiver, filter);
            final Intent i = new Intent(context, AccessibilityListener.class);
            String nodeId = call.argument("nodeId");
            i.putExtra(GET_EVENT_BY_TEXT, true);
            i.putExtra("text", nodeId);
            context.startService(i);
            /*
            String textNodeValues = i.getStringExtra("textNodeValues");
            System.out.println("内部运行取值"+textNodeValues);
            result.success("运行内容");*/

        }else if(call.method.equals("fvid")) {
            //0329 根据nodeId查找Node节点 GET_EVENT_BY_TEXT
            final Intent i = new Intent(context, AccessibilityListener.class);
            String viewId = call.argument("viewId");
            Boolean needClick = call.argument("needClick");
            int clickNum = call.argument("clickNum");
            i.putExtra(GET_EVENT_BY_VIEW_ID, true);
            i.putExtra("viewId", viewId);
            i.putExtra("needClick",needClick);
            i.putExtra("clickNum",clickNum);
            context.startService(i);

        } else if (call.method.equals("performGlobalAction")) {
            //截屏
            Integer actionId = call.argument("action");
            if (Utils.isAccessibilitySettingsOn(context)) {
                final Intent i = new Intent(context, AccessibilityListener.class);
                i.putExtra(INTENT_GLOBAL_ACTION, true);
                i.putExtra(INTENT_GLOBAL_ACTION_ID, actionId);
                context.startService(i);
                result.success(true);
            } else {
                result.success(false);
            }
        }else if (call.method.equals("touchPoint")) {
            if (Utils.isAccessibilitySettingsOn(context)) {
                double x = call.argument("x");
                double y = call.argument("y");
                float[] position = {(float) x, (float) y};

                final Intent i = new Intent(context, AccessibilityListener.class);
                i.putExtra(TOUCH_POINT_ACTION, true);
                i.putExtra(INTENT_GLOBAL_ACTION_ID, 20);
                i.putExtra("positionA", position);
                context.startService(i);
                System.out.println("===完成点击 Java " + x + " - " + y);
                result.success(true);
            } else {
                result.success(false);
            }

        }else if (call.method.equals("slidePoint")) {
            if (Utils.isAccessibilitySettingsOn(context)) {
                double x = call.argument("x");
                double x1 = call.argument("x1");
                double y = call.argument("y");
                double y1 = call.argument("y1");
                float[] position = {(float) x, (float) y,(float) x1, (float) y1};

                final Intent i = new Intent(context, AccessibilityListener.class);
                i.putExtra(SLIDE_POINT_ACTION, true);
                i.putExtra(INTENT_GLOBAL_ACTION_ID, 21);
                i.putExtra("positionA", position);
                context.startService(i);
                System.out.println("===完成滑动 Java " + x + " - " + y);
                result.success(true);
            } else {
                result.success(false);
            }

        } else if (call.method.equals("performActionById")) {
            String nodeId = call.argument("nodeId");
            Integer action = (Integer) call.argument("nodeAction");
            Object extras = call.argument("extras");
            Bundle arguments = Utils.bundleIdentifier(action, extras);
            AccessibilityNodeInfo nodeInfo = AccessibilityListener.getNodeInfo(nodeId);
            if (nodeInfo != null) {
                if (arguments == null) {
                    nodeInfo.performAction(action);
                } else {
                    nodeInfo.performAction(action, arguments);
                }
                result.success(true);
            } else {
                result.success(false);
            }
        }else if(call.method.equals("pasteTxt")){
            Bundle arguments = new Bundle();
            String nodeId = call.argument("nodeId");
            String txt = call.argument("txt");

            AccessibilityNodeInfo info = AccessibilityListener.getNodeInfo(nodeId);
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, txt);
            if(info!=null){
                System.out.println("=====++============当前nodeId节点粘贴： "+nodeId );
                //info.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                //info.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            }else{
                System.out.println("=====++============当前nodeId节点为空： "+nodeId );
            }
        }
     /*   else if (call.method.equals("performActionByText")) {
            String text = call.argument("text");
            Integer action = (Integer) call.argument("nodeAction");
            Object extras = call.argument("extras");
            Bundle arguments = Utils.bundleIdentifier(action, extras);
            AccessibilityNodeInfo nodeInfo = AccessibilityListener.getNodeInfo();
            if (nodeInfo != null) {
                AccessibilityNodeInfo nodeToClick = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    nodeToClick = Utils.findNodeByText(nodeInfo, text);
                }
                if (nodeToClick != null) {
                    if (arguments == null) {
                        nodeToClick.performAction(action);
                    } else {
                        nodeToClick.performAction(action, arguments);
                    }
                    result.success(true);
                } else {
                    result.success(false);
                }
            } else {
                result.success(false);
            }
        } */
        else if (call.method.equals("showOverlayWindow")) {
            if (!supportOverlay) {
                result.error("ERR:OVERLAY", "Add the overlay entry point to be able of using it", null);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                AccessibilityListener.showOverlay();
                result.success(true);
            } else {
                result.success(false);
            }
        } else if (call.method.equals("hideOverlayWindow")) {
            AccessibilityListener.removeOverlay();
            result.success(true);
        } else {
            result.notImplemented();
        }
    }

    /**
     * 递归查找当前聊天窗口中的内容
     * @param node
     */
    public AccessibilityNodeInfo recycle(AccessibilityNodeInfo node,String name) {
        if (node.getChildCount() == 0) {
            if (node.getText() != null) {
                if (name.equals(node.getText().toString())) {
                    return node;
                }
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    recycle(node.getChild(i),name);
                }
            }
        }
        return node;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
        context.unregisterReceiver(actionsReceiver);
        context.unregisterReceiver(actionsEventReceiver);
    }
    @SuppressLint("WrongConstant")
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        if (Utils.isAccessibilitySettingsOn(context)) {
            /// Set up receiver
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACCESSIBILITY_INTENT);

            accessibilityReceiver = new AccessibilityReceiver(events);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.registerReceiver(accessibilityReceiver, intentFilter, Context.RECEIVER_EXPORTED);
            }else{
                context.registerReceiver(accessibilityReceiver, intentFilter);
            }

            /// Set up listener intent
            Intent listenerIntent = new Intent(context, AccessibilityListener.class);
            context.startService(listenerIntent);
            Log.i("AccessibilityPlugin", "Started the accessibility tracking service.");
        }
    }

    @Override
    public void onCancel(Object arguments) {
        context.unregisterReceiver(accessibilityReceiver);
        accessibilityReceiver = null;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOR_ACCESSIBILITY) {
            if (resultCode == Activity.RESULT_OK) {
                pendingResult.success(true);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                pendingResult.success(Utils.isAccessibilitySettingsOn(context));
            } else {
                pendingResult.success(false);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.mActivity = binding.getActivity();
        binding.addActivityResultListener(this);
        try {
            FlutterEngineGroup enn = new FlutterEngineGroup(context);
            DartExecutor.DartEntrypoint dEntry = new DartExecutor.DartEntrypoint(
                    FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                    "accessibilityOverlay");
            FlutterEngine engine = enn.createAndRunEngine(context, dEntry);
            FlutterEngineCache.getInstance().put(CACHED_TAG, engine);
            supportOverlay = true;
        } catch (Exception exception) {
            supportOverlay = false;
            Log.e("ENGINE-ERROR", "onAttachedToActivity: " + exception.getMessage());
        }
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.mActivity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        this.mActivity = null;
    }

    private void touchPoint(float x, float y, boolean canSwipe) {
        float[] position = {x, y};


        Intent intent = new Intent(context, AccessibilityListener.class);
        intent.putExtra("position", position);
        intent.putExtra("canSwipe", canSwipe);

        context.startService(intent);
    }
}
