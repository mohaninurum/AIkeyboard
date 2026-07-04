import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class SoundsScreen extends StatefulWidget {
  const SoundsScreen({super.key});

  @override
  State<SoundsScreen> createState() => _SoundsScreenState();
}

class _SoundsScreenState extends State<SoundsScreen> {
  static const platform = MethodChannel('com.example.keyboard/settings');
  String _currentSound = 'default';
  bool _isSoundEnabled = true;
  bool _isHapticEnabled = true;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    try {
      final settings = await platform.invokeMapMethod<String, dynamic>('getSettings');
      if (settings != null && mounted) {
        setState(() {
          _currentSound = settings['sound'] as String? ?? 'default';
          _isSoundEnabled = settings['soundEnabled'] as bool? ?? true;
          _isHapticEnabled = settings['hapticEnabled'] as bool? ?? true;
          _isLoading = false;
        });
      }
    } on PlatformException catch (e) {
      debugPrint("Failed to load settings: '\${e.message}'.");
      if (mounted) setState(() => _isLoading = false);
    }
  }

  final List<Map<String, String>> _sounds = [
    {'name': 'Default Click', 'id': 'default', 'icon': 'touch_app'},
    {'name': 'Mechanical', 'id': 'mechanical', 'icon': 'keyboard'},
    {'name': 'Typewriter', 'id': 'typewriter', 'icon': 'type_specimen'},
    {'name': 'Water Drop', 'id': 'water_drop', 'icon': 'water_drop'},
    {'name': 'Wood Tap', 'id': 'wood', 'icon': 'forest'},
    {'name': 'None (Mute)', 'id': 'none', 'icon': 'volume_off'},
  ];

  IconData _getIconData(String iconName) {
    switch (iconName) {
      case 'touch_app':
        return Icons.touch_app;
      case 'keyboard':
        return Icons.keyboard;
      case 'type_specimen':
        return Icons.type_specimen;
      case 'water_drop':
        return Icons.water_drop;
      case 'forest':
        return Icons.forest;
      case 'volume_off':
        return Icons.volume_off;
      default:
        return Icons.music_note;
    }
  }

  Future<void> _applySound(String soundId) async {
    try {
      await platform.invokeMethod('setSound', {'soundName': soundId});
      setState(() {
        _currentSound = soundId;
      });
      // Play demo sound
      await platform.invokeMethod('playDemoSound', {'soundName': soundId});
      
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Keypress sound updated!'),
          duration: Duration(seconds: 1),
        ),
      );
    } on PlatformException catch (e) {
      debugPrint("Failed to set sound: '\${e.message}'.");
    }
  }

  Future<void> _toggleSound(bool value) async {
    try {
      await platform.invokeMethod('setSoundEnabled', {'enabled': value});
      setState(() {
        _isSoundEnabled = value;
      });
    } on PlatformException catch (e) {
      debugPrint("Failed to toggle sound: '\${e.message}'.");
    }
  }

  Future<void> _toggleHaptic(bool value) async {
    try {
      await platform.invokeMethod('setHapticEnabled', {'enabled': value});
      setState(() {
        _isHapticEnabled = value;
      });
    } on PlatformException catch (e) {
      debugPrint("Failed to toggle haptic: '\${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Scaffold(
        appBar: AppBar(title: const Text('Keypress Sound')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }
    return Scaffold(
      appBar: AppBar(
        title: const Text('Keypress Sound'),
        centerTitle: true,
      ),
      body: Column(
        children: [
          SwitchListTile(
            title: const Text('Enable Keypress Sounds'),
            subtitle: const Text('Play sound when typing'),
            value: _isSoundEnabled,
            secondary: const Icon(Icons.volume_up),
            onChanged: _toggleSound,
          ),
          SwitchListTile(
            title: const Text('Enable Haptic Feedback'),
            subtitle: const Text('Vibrate when typing'),
            value: _isHapticEnabled,
            secondary: const Icon(Icons.vibration),
            onChanged: _toggleHaptic,
          ),
          const Divider(height: 1),
          Expanded(
            child: ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: _sounds.length,
              separatorBuilder: (context, index) => const Divider(),
              itemBuilder: (context, index) {
                final sound = _sounds[index];
                final isSelected = _currentSound == sound['id'];
                
                // If sound is disabled, make the list look disabled
                final opacity = _isSoundEnabled ? 1.0 : 0.4;
                
                return Opacity(
                  opacity: opacity,
                  child: ListTile(
                    enabled: _isSoundEnabled,
                    leading: CircleAvatar(
                      backgroundColor: isSelected
                          ? Theme.of(context).colorScheme.primaryContainer
                          : Theme.of(context).colorScheme.surfaceContainerHighest,
                      child: Icon(
                        _getIconData(sound['icon']!),
                        color: isSelected
                            ? Theme.of(context).colorScheme.onPrimaryContainer
                            : Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                    title: Text(
                      sound['name']!,
                      style: TextStyle(
                        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                      ),
                    ),
                    trailing: isSelected
                        ? const Icon(Icons.check_circle, color: Colors.green)
                        : const Icon(Icons.play_circle_outline, color: Colors.grey),
                    onTap: _isSoundEnabled ? () => _applySound(sound['id']!) : null,
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
