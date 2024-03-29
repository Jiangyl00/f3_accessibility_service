import 'dart:async';
import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:flutter_accessibility_service/accessibility_event.dart';
import 'package:flutter_accessibility_service/constants.dart';
import 'package:flutter_accessibility_service/flutter_accessibility_service.dart';

import 'package:flutter_accessibility_service_example/overlay.dart';

import 'package:collection/collection.dart';

@pragma("vm:entry-point")
void accessibilityOverlay() {
  runApp(
    const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: BlockingOverlay(),
    ),
  );
}

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  StreamSubscription<AccessibilityEvent>? _subscription;
  List<AccessibilityEvent?> events = [];
  DateTime eventDateTime = DateTime.now();
  bool foundSearchField = false;
  bool setText = false;
  bool clickFirstSearch = false;

  @override
  void initState() {
    super.initState();
  }

  void handleAccessiblityStream() {
    foundSearchField = false;
    setText = false;
    if (_subscription?.isPaused ?? false) {
      _subscription?.resume();
      return;
    }
    _subscription =
        FlutterAccessibilityService.accessStream.listen((event) async {
      setState(() {
        events.add(event);
      });
      // automateScroll(event);
      // log("$event");
      // automateWikipedia(event);
      wxAutomateScroll(event);
      handleOverlay(event);
    });
  }
  int action_n = 0;

  //当前页面 0主页 1会话页
  int nowPage = 0;
  int nowTouchNum = 0;

  late Timer timer;
  void wxAutomateScroll(AccessibilityEvent node)  {

    if (!node.packageName!.contains('com.tencent.mm')) return;
    //log("$node");
    if(action_n!=0 ) {
      print("-------------当前程序执行中--------------");
      return;
    }

    print("微信自动点击方法  包名：${node.packageName}");


/*    await FlutterAccessibilityService.performGlobalAction(
      GlobalAction.globalActionBack,
    );*/

    final scrollableNode = findScrollableNode(node);
    //log('$scrollableNode', name: 'SCROLLABLE- XX');
    /*if (scrollableNode != null) {
        await FlutterAccessibilityService.performAction(
          node,
          NodeAction.actionScrollForward,
        );
      }*/
    for(int a=0;a<node.subNodes!.length;a++){
      print("当前ID: ${node.subNodes![a].mapId} nodeID: ${node.subNodes![a].nodeId} type: ${node.subNodes![a].actionType}  text:${node.subNodes![a].text}");
      /*if(node.subNodes![a].nodeId == "com.tencent.mm:id/kbq"){
          timer =  Timer(Duration(milliseconds: 2000), () async {
            print("点击 ${node.subNodes![a].mapId}");
            await FlutterAccessibilityService.performAction(
              node,
              NodeAction.actionClick,
            );
          });
        }
        */

      if(node.subNodes![a].nodeId == "com.tencent.mm:id/kbq" &&( node.subNodes![a].mapId!.contains("听风讲你")|| node.subNodes![a].mapId!.contains("小小酥油茶"))){
        //automateScroll(node.subNodes![a]);
        print("--点击-- ${node.subNodes![a].mapId}");
        ScreenBounds screenBounds = node.subNodes![a].screenBounds!;
        int left = screenBounds.left??0;
        int top = screenBounds.top??0;
         FlutterAccessibilityService.slidePoint(x: left.toDouble()+50, y: top.toDouble()+250,x1: left.toDouble()+80, y1: top.toDouble()+150);
        nowPage = 1;
        nowTouchNum = 0;
        print("---------点击事件响应--------- x: ${left.toDouble()+50} y: ${top.toDouble()+50}- -----------");

       /* await FlutterAccessibilityService.performAction(
          node.subNodes![a],
          NodeAction.actionFocus,
        );
        await FlutterAccessibilityService.performAction(
          node,
          NodeAction.actionClick,
        );*/
      }

      if(node.subNodes![a].nodeId == "com.tencent.mm:id/bkk" && node.subNodes![a].mapId!.contains("EditText")){

        print("--点击-- ${node.subNodes![a].mapId}");
        ScreenBounds screenBounds = node.subNodes![a].screenBounds!;
        int left = screenBounds.left??0;
        int top = screenBounds.top??0;
        if(nowTouchNum == 0){
          nowTouchNum = nowTouchNum+1;
          FlutterAccessibilityService.touchPoint(x: left.toDouble()+50, y: top.toDouble()+50) ;
          print("---------点击事件响应--------- x: ${left.toDouble()+50} y: ${top.toDouble()+50}------------");

          Future.delayed(Duration(seconds: 2), () {  FlutterAccessibilityService.pasteTxt(nodeId: node.subNodes![a].mapId!,txt: "wbd" ); });


         // FlutterAccessibilityService.pasteTxt(nodeId: node.subNodes![a].mapId!,txt: "wbd" );
          print("---------粘贴事件响应--------- --- ${node.subNodes![a].nodeId}  -------");
          Future.delayed(Duration(seconds: 2), () {  print("------ ${DateTime.now()}"); });
          Future.delayed(Duration(seconds: 2), () {  print("------ ${DateTime.now()}"); });
          //返回上一页 返回两次 。 第一次为隐藏键盘
          //Future.delayed(Duration(seconds: 1), () {  FlutterAccessibilityService.performGlobalAction(GlobalAction.globalActionBack,); });
          //Future.delayed(Duration(seconds: 1), () {  FlutterAccessibilityService.performGlobalAction(GlobalAction.globalActionBack,); });

          //FlutterAccessibilityService.performGlobalAction(GlobalAction.globalActionBack,);
        }
        nowTouchNum = nowTouchNum+1;



       /* await FlutterAccessibilityService.performAction(
          node.subNodes![a],
          NodeAction.actionFocus,
        );
        await FlutterAccessibilityService.performAction(
          node,
          NodeAction.actionClick,
        );*/
      }


    }

  }
  void handleOverlay(AccessibilityEvent event) async {
    if (event.packageName!.contains('youtube')) {
      log('$event');
    }
    if (event.packageName!.contains('youtube') && event.isFocused!) {
      eventDateTime = event.eventTime!;
      await FlutterAccessibilityService.showOverlayWindow();
    } else if (eventDateTime.difference(event.eventTime!).inSeconds.abs() > 2 ||
        (event.eventType == EventType.typeWindowStateChanged &&
            !event.isFocused!)) {
      await FlutterAccessibilityService.hideOverlayWindow();
    }
  }

  void automateWikipedia(AccessibilityEvent event) async {
    if (!event.packageName!.contains('wikipedia')) return;
    log('$event');
    final searchIt = [...event.subNodes!, event].firstWhereOrNull(
      (element) => element.text == 'Search Wikipedia' && element.isClickable!,
    );
    log("Searchable Field: $searchIt");
    if (searchIt != null) {
      await doAction(searchIt, NodeAction.actionClick);
      final editField = [...event.subNodes!, event].firstWhereOrNull(
        (element) => element.text == 'Search Wikipedia' && element.isEditable!,
      );
      if (editField != null) {
        await doAction(editField, NodeAction.actionSetText, "Lionel Messi");
      }
      final item = [...event.subNodes!, event].firstWhereOrNull(
        (element) => element.text == 'Messi–Ronaldo rivalry',
      );
      if (item != null) {
        await doAction(item, NodeAction.actionSelect);
      }
    }
  }

  Future<bool> doAction(
    AccessibilityEvent node,
    NodeAction action, [
    dynamic argument,
  ]) async {
    return await FlutterAccessibilityService.performAction(
      node,
      action,
      argument,
    );
  }

  void automateScroll(AccessibilityEvent node) async {
    if (!node.packageName!.contains('youtube')) return;
    log("$node");
    if (node.isFocused!) {
      final scrollableNode = findScrollableNode(node);
      log('$scrollableNode', name: 'SCROLLABLE- XX');
      if (scrollableNode != null) {
        await FlutterAccessibilityService.performAction(
          node,
          NodeAction.actionScrollForward,
        );
      }
    }
  }

  AccessibilityEvent? findScrollableNode(AccessibilityEvent rootNode) {
    if (rootNode.isScrollable! &&
        rootNode.actions!.contains(NodeAction.actionScrollForward)) {
      return rootNode;
    }
    if (rootNode.subNodes!.isEmpty) return null;
    for (int i = 0; i < rootNode.subNodes!.length; i++) {
      final childNode = rootNode.subNodes![i];
      final scrollableChild = findScrollableNode(childNode);
      if (scrollableChild != null) {
        return scrollableChild;
      }
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    TextButton(
                      onPressed: () async {
                        await FlutterAccessibilityService
                            .requestAccessibilityPermission();
                      },
                      child: const Text("Request Permission"),
                    ),
                    const SizedBox(height: 20.0),
                    TextButton(
                      onPressed: () async {
                        final bool res = await FlutterAccessibilityService
                            .isAccessibilityPermissionEnabled();
                        log("Is enabled: $res");
                      },
                      child: const Text("Check Permission"),
                    ),
                    const SizedBox(height: 20.0),
                    TextButton(
                      onPressed: handleAccessiblityStream,
                      child: const Text("Start Stream"),
                    ),
                    const SizedBox(height: 20.0),
                    TextButton(
                      onPressed: () {
                        _subscription?.cancel();
                      },
                      child: const Text("Stop Stream"),
                    ),
                    TextButton(
                      onPressed: () async {
                        await FlutterAccessibilityService.performGlobalAction(
                          GlobalAction.globalActionTakeScreenshot,
                        );
                      },
                      child: const Text("Take ScreenShot"),
                    ),
                    TextButton(
                      onPressed: () async {
                        final list = await FlutterAccessibilityService
                            .getSystemActions();
                        log('$list');
                      },
                      child: const Text("List GlobalActions"),
                    ),
                  ],
                ),
              ),
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  TextButton(
                    onPressed: () {

                      FlutterAccessibilityService.slidePoint(x: 900, y: 900,x1: 100, y1: 100,);
                    },
                    child: const Text("点击执行"),
                  ),
                  const SizedBox(height: 20.0),
                ],
              ),
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  TextButton(
                    onPressed: () async {
                      await Future.delayed(Duration(milliseconds: 5000), () {
                        var findAccessibilityNodeInfosByText = FlutterAccessibilityService.findAccessibilityNodeInfosByText(nodeId:"首页") ;

                        print("发送按钮完成 第三次返回 $findAccessibilityNodeInfosByText");
                      });

                    },
                    child: const Text("    点击获取节点"),
                  ),
                  const SizedBox(height: 20.0),
                ],
              ),
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  TextButton(
                    onPressed: () async {
                      await Future.delayed(Duration(milliseconds: 5000), () {
                        var findAccessibilityNodeInfosByText = FlutterAccessibilityService.findAccessibilityNodeInfosByViewId(viewId:"@id/kai") ;

                        print("发送按钮完成 第三次返回 $findAccessibilityNodeInfosByText");
                      });

                    },
                    child: const Text("    点击获取ViewId"),
                  ),
                  const SizedBox(height: 20.0),
                ],
              ),
              Expanded(
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: events.length,
                  itemBuilder: (_, index) => ListTile(
                    title: Text(events[index]!.packageName!),
                    subtitle: Text(
                      (events[index]!.subNodes ?? [])
                              .map((e) => e.actions)
                              .expand((element) => element!)
                              .contains(NodeAction.actionClick)
                          ? 'Have Action to click'
                          : '',
                    ),
                  ),
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}
