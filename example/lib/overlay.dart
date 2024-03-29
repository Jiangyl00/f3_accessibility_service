import 'package:flutter/material.dart';

class BlockingOverlay extends StatelessWidget {
  const BlockingOverlay({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Material(

      color: Colors.white,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image.network(
              'https://ask.qcloudimg.com/http-save/yehe-2608304/f12f34uo0m.jpeg',
              width: 120.0,
            ),
            const SizedBox(height: 15.0),
            const Text(
              'Ayo here me out',
              style: TextStyle(
                color: Colors.black,
                fontSize: 25.0,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
