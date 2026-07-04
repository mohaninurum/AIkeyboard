import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class ThemesScreen extends StatelessWidget {
  const ThemesScreen({super.key});

  static const platform = MethodChannel('com.example.keyboard/settings');

  Future<void> _applyTheme(BuildContext context, String themeName) async {
    try {
      await platform.invokeMethod('setTheme', {'themeName': themeName});
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$themeName Theme Applied!')),
      );
    } on PlatformException catch (e) {
      debugPrint("Failed to set theme: '\${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    final themes = [
      {
        'name': 'Neon Cyan',
        'id': 'neon_cyan',
        'bgColor': Colors.black,
        'glowColor': Colors.cyanAccent,
      },
      {
        'name': 'Neon Green',
        'id': 'neon_green',
        'bgColor': const Color(0xFF111111),
        'glowColor': Colors.greenAccent,
      },
      {
        'name': 'Cyberpunk',
        'id': 'cyberpunk',
        'bgColor': const Color(0xFF2B0033),
        'glowColor': Colors.pinkAccent,
      },
      {
        'name': 'Blood Red',
        'id': 'blood_red',
        'bgColor': Colors.black,
        'glowColor': Colors.redAccent,
      }
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Keyboard Themes'),
        centerTitle: true,
      ),
      body: GridView.builder(
        padding: const EdgeInsets.all(16),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          crossAxisSpacing: 16,
          mainAxisSpacing: 16,
          childAspectRatio: 0.75,
        ),
        itemCount: themes.length,
        itemBuilder: (context, index) {
          final theme = themes[index];
          return Card(
            elevation: 4,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: Container(
                    decoration: BoxDecoration(
                      color: theme['bgColor'] as Color,
                      borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
                    ),
                    child: Center(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                        decoration: BoxDecoration(
                          color: Colors.white10,
                          borderRadius: BorderRadius.circular(8),
                          boxShadow: [
                            BoxShadow(
                              color: (theme['glowColor'] as Color).withOpacity(0.6),
                              blurRadius: 15,
                              spreadRadius: 2,
                            ),
                          ],
                        ),
                        child: Text(
                          'A',
                          style: TextStyle(
                            color: theme['glowColor'] as Color,
                            fontSize: 32,
                            fontWeight: FontWeight.bold,
                            shadows: [
                              Shadow(
                                color: theme['glowColor'] as Color,
                                blurRadius: 10,
                              )
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: Column(
                    children: [
                      Text(
                        theme['name'] as String,
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 8),
                      ElevatedButton(
                        onPressed: () => _applyTheme(context, theme['id'] as String),
                        style: ElevatedButton.styleFrom(
                          minimumSize: const Size.fromHeight(36),
                        ),
                        child: const Text('Apply'),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
