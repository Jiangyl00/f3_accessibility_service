import 'package:android_window/android_window.dart';
import 'package:flutter/material.dart';

class AndroidWindowApp extends StatelessWidget {
  const AndroidWindowApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: HomePage(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return AndroidWindow(
      child: Scaffold(
        backgroundColor: Colors.lightGreen.withOpacity(0.9),
        body: const Padding(
          padding: EdgeInsets.all(8),
          child: Text('Hello android AB'),
        ),
      ),
    );
  }
}