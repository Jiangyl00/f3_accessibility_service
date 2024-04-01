package slayer.accessibility.service.flutter_accessibility_service;

import static slayer.accessibility.service.flutter_accessibility_service.Constants.*;
import static slayer.accessibility.service.flutter_accessibility_service.FlutterAccessibilityServicePlugin.CACHED_TAG;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.RequiresApi;


import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import io.flutter.embedding.android.FlutterTextureView;
import io.flutter.embedding.android.FlutterView;
import io.flutter.embedding.engine.FlutterEngineCache;
//123

public class AccessibilityListener extends AccessibilityService {
    private static WindowManager mWindowManager;
    private static FlutterView mOverlayView;
    static private boolean isOverlayShown = false;
    private static final int CACHE_SIZE = 4000;
    private static LruCache<String, AccessibilityNodeInfo> nodeMap =
            new LruCache<>(CACHE_SIZE);

    public static AccessibilityNodeInfo getNodeInfo(String id) {
        return nodeMap.get(id);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        try {
            final int eventType = accessibilityEvent.getEventType();
            AccessibilityNodeInfo parentNodeInfo = accessibilityEvent.getSource();
            AccessibilityWindowInfo windowInfo = null;
            List<String> nextTexts = new ArrayList<>();
            List<Integer> actions = new ArrayList<>();
            List<HashMap<String, Object>> subNodeActions = new ArrayList<>();
            HashSet<AccessibilityNodeInfo> traversedNodes = new HashSet<>();
            HashMap<String, Object> data = new HashMap<>();
            if (parentNodeInfo == null) {
                return;
            }
            String nodeId = generateNodeId(parentNodeInfo);
            String packageName = parentNodeInfo.getPackageName().toString();
            storeNode(nodeId, parentNodeInfo);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                windowInfo = parentNodeInfo.getWindow();
            }


            Intent intent = new Intent(ACCESSIBILITY_INTENT);
            data.put("windowId", parentNodeInfo.getWindowId());
            data.put("text", parentNodeInfo.getText());
            data.put("contentDescription", parentNodeInfo.getContentDescription());
            data.put("className", parentNodeInfo.getClassName());

            data.put("mapId", nodeId);
            data.put("packageName", packageName);
            data.put("eventType", eventType);
            data.put("actionType", accessibilityEvent.getAction());
            data.put("eventTime", accessibilityEvent.getEventTime());
            data.put("movementGranularity", accessibilityEvent.getMovementGranularity());
            Rect rect = new Rect();
            parentNodeInfo.getBoundsInScreen(rect);
            data.put("screenBounds", getBoundingPoints(rect));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                data.put("contentChangeTypes", accessibilityEvent.getContentChangeTypes());
            }
            if (parentNodeInfo.getText() != null) {
                data.put("capturedText", parentNodeInfo.getText().toString());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                data.put("nodeId", parentNodeInfo.getViewIdResourceName());
            }
            getSubNodes(parentNodeInfo, subNodeActions, traversedNodes);
            data.put("nodesText", nextTexts);
            actions.addAll(parentNodeInfo.getActionList().stream().map(AccessibilityNodeInfo.AccessibilityAction::getId).collect(Collectors.toList()));
            data.put("parentActions", actions);
            data.put("subNodesActions", subNodeActions);
            data.put("isClickable", parentNodeInfo.isClickable());
            data.put("isScrollable", parentNodeInfo.isScrollable());
            data.put("isFocusable", parentNodeInfo.isFocusable());
            data.put("isCheckable", parentNodeInfo.isCheckable());
            data.put("isLongClickable", parentNodeInfo.isLongClickable());
            data.put("isEditable", parentNodeInfo.isEditable());
            if (windowInfo != null) {
                data.put("isActive", windowInfo.isActive());
                data.put("isFocused", windowInfo.isFocused());
                data.put("windowType", windowInfo.getType());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    data.put("isPip", windowInfo.isInPictureInPictureMode());
                }
            }
            storeToSharedPrefs(data);
            intent.putExtra(SEND_BROADCAST, true);
            sendBroadcast(intent);
        } catch (Exception ex) {
            Log.e("EVENT", "onAccessibilityEvent: " + ex.getMessage());
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean globalAction = intent.getBooleanExtra(INTENT_GLOBAL_ACTION, false);
        boolean touchAction = intent.getBooleanExtra(TOUCH_POINT_ACTION, false);
        boolean slideAction = intent.getBooleanExtra(SLIDE_POINT_ACTION, false);
        boolean systemActions = intent.getBooleanExtra(INTENT_SYSTEM_GLOBAL_ACTIONS, false);
        boolean getEventByText = intent.getBooleanExtra(GET_EVENT_BY_TEXT, false);
        boolean getEventByViewId = intent.getBooleanExtra(GET_EVENT_BY_VIEW_ID, false);


        if (systemActions && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            List<Integer> actions = getSystemActions().stream().map(AccessibilityNodeInfo.AccessibilityAction::getId).collect(Collectors.toList());
            Intent broadcastIntent = new Intent(BROD_SYSTEM_GLOBAL_ACTIONS);
            broadcastIntent.putIntegerArrayListExtra("actions", new ArrayList<>(actions));
            sendBroadcast(broadcastIntent);
        }

        if (globalAction) {
            int actionId = intent.getIntExtra(INTENT_GLOBAL_ACTION_ID, 8);
            performGlobalAction(actionId);
        }

        if (touchAction) {
            float[] positionA = intent.getFloatArrayExtra("positionA");
            try{
                ArrayList<Integer> positionArray = intent.getIntegerArrayListExtra("positionArray");
                System.out.println(positionArray==null);
            }catch (Exception e){
                System.out.println("未获取到点击positionArray坐标。。。");
            }
            System.out.println(positionA==null);
            if(positionA!=null){
                doTouch(positionA[0], positionA[1], intent.getBooleanExtra("canSwipe", false));
            }else{
                System.out.println("未获取到点击坐标。。。");
            }
        }

        if (slideAction) {
            float[] positionA = intent.getFloatArrayExtra("positionA");
            System.out.println(positionA==null);
            if(positionA!=null){
                doSlide(positionA[0], positionA[1], positionA[2], positionA[3]);
            }else{
                System.out.println("未获取到点击坐标。。。");
            }
        }

        if(getEventByText){
            //获取text对应节点
            Intent broadcastIntent = new Intent(BROD_EVENT_ITEMS_ACTIONS);
            String text = intent.getStringExtra("text");
            Boolean needClick = intent.getBooleanExtra("needClick",false);
            Boolean querySub = intent.getBooleanExtra("querySub",false);
            int clickNum = intent.getIntExtra("clickNum",0);
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if(nodeInfo == null){
                System.out.println("未找到对应节点" + text);
            }else{
                List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(text);
                System.out.println(text+"对应节点长度 "+list.size());
                ArrayList<HashMap<String, Object>> nodeInfo2Map = getNodeInfo2Map(list,querySub,needClick,0);
                broadcastIntent.putExtra("actionsnode", nodeInfo2Map);
                sendBroadcast(broadcastIntent);
            }

        }
        if(getEventByViewId){
            //获取text对应节点
            String viewId = intent.getStringExtra("viewId");
            Boolean needClick = intent.getBooleanExtra("needClick",false);
            Boolean querySub = intent.getBooleanExtra("querySub",false);
            int clickNum = intent.getIntExtra("clickNum",0);
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo == null) {
                System.out.println("未找到对应节点 "+viewId);
            }else{
                //为了演示,直接查看了关闭按钮的id
                //获取text对应节点
                Intent broadcastIntent = new Intent(BROD_EVENT_ITEMS_ACTIONS);
                List<AccessibilityNodeInfo> infos = nodeInfo.findAccessibilityNodeInfosByViewId(viewId);
                System.out.println(viewId+"对应节点长度 "+infos.size());
                ArrayList<HashMap<String, Object>> nodeInfo2Map = getNodeInfo2Map(infos,querySub,needClick,clickNum);
                broadcastIntent.putExtra("actionsnode", nodeInfo2Map);
                sendBroadcast(broadcastIntent);
            }
        }

        Log.d("命令开始", "执行命令ID : " + startId);
        return START_STICKY;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    ArrayList<HashMap<String,Object>> getNodeInfo2Map(List<AccessibilityNodeInfo> list,boolean querySub,boolean needClick,int clickNum){
        ArrayList<HashMap<String,Object>> rm = new ArrayList<>();
        for(int a=0;a<list.size();a++){
            AccessibilityWindowInfo windowInfo = null;
            AccessibilityNodeInfo parentNodeInfo = list.get(a);
            HashMap<String, Object> data = new HashMap<>();

            String nodeId = generateNodeId(parentNodeInfo);
            String packageName = parentNodeInfo.getPackageName().toString();



            data.put("windowId", parentNodeInfo.getWindowId());
            data.put("text", parentNodeInfo.getText());
            data.put("contentDescription", parentNodeInfo.getContentDescription());
            data.put("className", parentNodeInfo.getClassName());
            data.put("mapId", nodeId);
            data.put("packageName", packageName);
/*              data.put("eventType", eventType);
                data.put("actionType", accessibilityEvent.getAction());
                data.put("eventTime", accessibilityEvent.getEventTime());
                data.put("movementGranularity", accessibilityEvent.getMovementGranularity());*/
            Rect rect = new Rect();
            parentNodeInfo.getBoundsInScreen(rect);
            data.put("screenBounds", getBoundingPoints(rect));
           /*     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    data.put("contentChangeTypes", accessibilityEvent.getContentChangeTypes());
                }*/
            if (parentNodeInfo.getText() != null) {
                data.put("capturedText", parentNodeInfo.getText().toString());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                data.put("nodeId", parentNodeInfo.getViewIdResourceName());
            }
            //data.put("nodesText", nextTexts);

            if(querySub){
                List<HashMap<String, Object>> subNodeActions = new ArrayList<>();
                HashSet<AccessibilityNodeInfo> traversedNodes = new HashSet<>();
                getSubNodes(parentNodeInfo, subNodeActions, traversedNodes);
                data.put("subNodesActions", subNodeActions);
            }

            List<Integer> actions = new ArrayList<>();
            actions.addAll(parentNodeInfo.getActionList().stream().map(AccessibilityNodeInfo.AccessibilityAction::getId).collect(Collectors.toList()));
            data.put("parentActions", actions);
            data.put("isClickable", parentNodeInfo.isClickable());
            data.put("isScrollable", parentNodeInfo.isScrollable());
            data.put("isFocusable", parentNodeInfo.isFocusable());
            data.put("isCheckable", parentNodeInfo.isCheckable());
            data.put("isLongClickable", parentNodeInfo.isLongClickable());
            data.put("isEditable", parentNodeInfo.isEditable());
            if (windowInfo != null) {
                data.put("isActive", windowInfo.isActive());
                data.put("isFocused", windowInfo.isFocused());
                data.put("windowType", windowInfo.getType());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    data.put("isPip", windowInfo.isInPictureInPictureMode());
                }
            }
            if(needClick && a == clickNum){
                boolean b = list.get(a).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                data.put("isClickedCz",b);
            }
            rm.add(data);
        }
        return rm;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void doTouch(float x, float y, boolean canSwipe) {

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        Path path = new Path();
        if (canSwipe) {
            path.moveTo(x, y);
            path.lineTo(x, y - 500);
        } else {
            path.moveTo(x, y);
        }
        System.out.println("============Java执行点击： x "+x + " y "+y);

        Random ran = new Random();
        int i = ran.nextInt(101);
        i += 100;
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, i, 100+i, false));

        dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.d("TAG", "手势已取消");
            }

            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d("TAG", "手势已完成");
            }
        }, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void doSlide(float x, float y,float x1,float y1) {

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        Path path = new Path();

        path.moveTo(x, y);
        path.lineTo(x1, y1);

        System.out.println("============Java执行滑动： x "+x  + " X1 "+x1+ " y "+y + " Y1 "+y1);
        Random ran = new Random();
        int i = ran.nextInt(101);
        i += 100;
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, i, 300+i, false));

        dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.d("TAG", "手势已取消");
            }

            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d("TAG", "手势已完成");
            }
        }, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void getSubNodes(AccessibilityNodeInfo node, List<HashMap<String, Object>> arr, HashSet<AccessibilityNodeInfo> traversedNodes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (traversedNodes.contains(node)) return;
            traversedNodes.add(node);
            String mapId = generateNodeId(node);
            AccessibilityWindowInfo windowInfo = null;
            HashMap<String, Object> nested = new HashMap<>();
            Rect rect = new Rect();
            node.getBoundsInScreen(rect);
            windowInfo = node.getWindow();
            nested.put("mapId", mapId);
            nested.put("nodeId", node.getViewIdResourceName());
            nested.put("capturedText", node.getText());
            nested.put("screenBounds", getBoundingPoints(rect));
            nested.put("isClickable", node.isClickable());
            nested.put("isScrollable", node.isScrollable());
            nested.put("isFocusable", node.isFocusable());
            nested.put("isCheckable", node.isCheckable());
            nested.put("isLongClickable", node.isLongClickable());
            nested.put("isEditable", node.isEditable());
            nested.put("parentActions", node.getActionList().stream().map(AccessibilityNodeInfo.AccessibilityAction::getId).collect(Collectors.toList()));
            if (windowInfo != null) {
                nested.put("isActive", node.getWindow().isActive());
                nested.put("isFocused", node.getWindow().isFocused());
                nested.put("windowType", node.getWindow().getType());
            }
            arr.add(nested);
            storeNode(mapId, node);
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child == null)
                    continue;
                getSubNodes(child, arr, traversedNodes);
            }
        }
    }

    private HashMap<String, Integer> getBoundingPoints(Rect rect) {
        HashMap<String, Integer> frame = new HashMap<>();
        frame.put("left", rect.left);
        frame.put("right", rect.right);
        frame.put("top", rect.top);
        frame.put("bottom", rect.bottom);
        frame.put("width", rect.width());
        frame.put("height", rect.height());
        return frame;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onServiceConnected() {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mOverlayView = new FlutterView(getApplicationContext(), new FlutterTextureView(getApplicationContext()));
        mOverlayView.attachToFlutterEngine(FlutterEngineCache.getInstance().get(CACHED_TAG));
        mOverlayView.setFitsSystemWindows(true);
        mOverlayView.setFocusable(true);
        mOverlayView.setFocusableInTouchMode(true);
        mOverlayView.setBackgroundColor(Color.TRANSPARENT);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    static public void showOverlay() {
        if (!isOverlayShown) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            lp.format = PixelFormat.TRANSLUCENT;
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            lp.gravity = Gravity.TOP;
            mWindowManager.addView(mOverlayView, lp);
            isOverlayShown = true;
        }
    }

    static public void removeOverlay() {
        if (isOverlayShown) {
            mWindowManager.removeView(mOverlayView);
            isOverlayShown = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeOverlay();
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(ACCESSIBILITY_NODE).commit();
    }

    @Override
    public void onInterrupt() {
    }


    private String generateNodeId(AccessibilityNodeInfo node) {
        return node.getWindowId() + "_" + node.getClassName() + "_" + node.getText() + "_" + node.getContentDescription(); //UUID.randomUUID().toString();
    }

    private void storeNode(String uuid, AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }
        nodeMap.put(uuid, node);
    }

    void storeToSharedPrefs(HashMap<String, Object> data) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(data);
        editor.putString(ACCESSIBILITY_NODE, json);
        editor.apply();
    }




}
