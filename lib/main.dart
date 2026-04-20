import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(const App());

class App extends StatelessWidget {
  const App({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'Widget Pro',
    theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
    home: const Home(),
    debugShowCheckedModeBanner: false,
  );
}

const _ch = MethodChannel('com.mydev.widgetpro/ch');

class AppInfo { final String name, pkg; AppInfo(this.name, this.pkg); }
class Folder {
  final String id, name; final List<String> apps;
  Folder({required this.id, required this.name, required this.apps});
  Map toJson() => {'id': id, 'name': name, 'apps': apps};
  factory Folder.from(Map j) => Folder(id: j['id'], name: j['name'], apps: List<String>.from(j['apps'] ?? []));
}

class Home extends StatefulWidget {
  const Home({super.key});
  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> with SingleTickerProviderStateMixin {
  late TabController _tab;
  List<AppInfo> apps = [];
  List<Map> notifs = [];
  List<Folder> folders = [];
  Set<String> allowed = {};
  int nr=30, ng=30, nb=46, na=200;
  int fr=26, fg=26, fb=46, fa=200;
  bool loading = true, firstLaunch = true;

  @override
  void initState() { super.initState(); _tab = TabController(length: 4, vsync: this); _load(); }
  @override
  void dispose() { _tab.dispose(); super.dispose(); }

  Future<void> _load() async {
    try {
      final d = jsonDecode(await _ch.invokeMethod('load') as String);
      setState(() {
        apps = (d['apps'] as List).map((a) => AppInfo(a['n'], a['p'])).toList();
        notifs = (d['notifs'] as List).map((n) => Map.from(n)).toList();
        folders = (d['folders'] as List).map((f) => Folder.from(Map.from(f))).toList();
        allowed = Set<String>.from(d['allowed'] ?? []);
        nr=d['nr']??30; ng=d['ng']??30; nb=d['nb']??46; na=d['na']??200;
        fr=d['fr']??26; fg=d['fg']??26; fb=d['fb']??46; fa=d['fa']??200;
        firstLaunch = d['firstLaunch'] ?? true;
        loading = false;
      });
      if (firstLaunch) WidgetsBinding.instance.addPostFrameCallback((_) => _pickApps());
    } catch (e) { setState(() => loading = false); }
  }

  Future<void> _saveNotif() => _ch.invokeMethod('saveNotif', {'allowed': allowed.toList(), 'a': na, 'r': nr, 'g': ng, 'b': nb});
  Future<void> _saveFolder() => _ch.invokeMethod('saveFolder', {'folders': jsonEncode(folders.map((f) => f.toJson()).toList()), 'a': fa, 'r': fr, 'g': fg, 'b': fb});

  void _pickApps() {
    showDialog(context: context, barrierDismissible: false, builder: (ctx) => StatefulBuilder(builder: (ctx, set) {
      final all = allowed.length == apps.length;
      return AlertDialog(
        title: const Text('Choose Notification Apps'),
        content: SizedBox(width: double.maxFinite, height: 450, child: Column(children: [
          Row(children: [Checkbox(value: all, onChanged: (v) { set(() { if (v==true) allowed=apps.map((a)=>a.pkg).toSet(); else allowed.clear(); }); setState((){}); }), const Text('Select All', style: TextStyle(fontWeight: FontWeight.bold))]),
          const Divider(),
          Expanded(child: ListView(children: apps.map((a) => CheckboxListTile(title: Text(a.name), subtitle: Text(a.pkg, style: const TextStyle(fontSize: 10)), value: allowed.contains(a.pkg), onChanged: (v) { set(() { if (v==true) allowed.add(a.pkg); else allowed.remove(a.pkg); }); setState((){}); })).toList())),
        ])),
        actions: [TextButton(onPressed: () { _saveNotif(); Navigator.pop(ctx); }, child: const Text('Done'))],
      );
    }));
  }

  String _fmt(int ms) { final d=DateTime.fromMillisecondsSinceEpoch(ms); return '${d.hour.toString().padLeft(2,'0')}:${d.minute.toString().padLeft(2,'0')}'; }

  Color get _nc => Color.fromARGB(na, nr, ng, nb);
  Color get _fc => Color.fromARGB(fa, fr, fg, fb);

  Widget _colorRow(String label, int r, int g, int b, int a, Function(int,int,int,int) onChange) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(label, style: const TextStyle(fontWeight: FontWeight.bold)),
      Row(children: [
        const Text('R', style: TextStyle(color: Colors.red)),
        Expanded(child: Slider(value: r.toDouble(), min: 0, max: 255, onChanged: (v) => onChange(v.toInt(), g, b, a), activeColor: Colors.red)),
        Text('$r'),
      ]),
      Row(children: [
        const Text('G', style: TextStyle(color: Colors.green)),
        Expanded(child: Slider(value: g.toDouble(), min: 0, max: 255, onChanged: (v) => onChange(r, v.toInt(), b, a), activeColor: Colors.green)),
        Text('$g'),
      ]),
      Row(children: [
        const Text('B', style: TextStyle(color: Colors.blue)),
        Expanded(child: Slider(value: b.toDouble(), min: 0, max: 255, onChanged: (v) => onChange(r, g, v.toInt(), a), activeColor: Colors.blue)),
        Text('$b'),
      ]),
      Row(children: [
        const Icon(Icons.opacity, size: 16),
        Expanded(child: Slider(value: a.toDouble(), min: 0, max: 255, onChanged: (v) => onChange(r, g, b, v.toInt()))),
        Text('${(a/255*100).toInt()}%'),
      ]),
      Container(height: 32, decoration: BoxDecoration(color: Color.fromARGB(a, r, g, b), borderRadius: BorderRadius.circular(8), border: Border.all(color: Colors.grey))),
      const SizedBox(height: 8),
    ]);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Widget Pro'),
      actions: [
        IconButton(icon: const Icon(Icons.notifications_outlined), onPressed: () => _ch.invokeMethod('pinNotif')),
        IconButton(icon: const Icon(Icons.folder_outlined), onPressed: () => _ch.invokeMethod('pinFolder')),
        IconButton(icon: const Icon(Icons.photo_outlined), onPressed: () => _ch.invokeMethod('pinPhoto')),
        IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
      ],
      bottom: TabBar(controller: _tab, isScrollable: true, tabs: const [Tab(text: 'Notifications'), Tab(text: 'Folders'), Tab(text: 'Photos'), Tab(text: 'Settings')]),
    ),
    body: loading ? const Center(child: CircularProgressIndicator()) : TabBarView(controller: _tab, children: [_notifTab(), _folderTab(), _photoTab(), _settingsTab()]),
    floatingActionButton: ListenableBuilder(listenable: _tab, builder: (ctx, _) => _tab.index == 1
        ? FloatingActionButton.extended(onPressed: () => _editFolder(), icon: const Icon(Icons.create_new_folder_outlined), label: const Text('New Folder'))
        : const SizedBox.shrink()),
  );

  Widget _notifTab() {
    if (notifs.isEmpty) return const Center(child: Text('No notifications yet...', style: TextStyle(color: Colors.grey)));
    return ListView.builder(itemCount: notifs.length, itemBuilder: (_, i) {
      final n = notifs[i];
      return Card(margin: const EdgeInsets.symmetric(horizontal: 10, vertical: 4), child: Padding(padding: const EdgeInsets.all(10), child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Icon(Icons.notifications, color: Colors.indigo, size: 20),
        const SizedBox(width: 8),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [Expanded(child: Text(n['a']??'', style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.indigo, fontSize: 13))), Text(_fmt(n['ts']??0), style: const TextStyle(color: Colors.grey, fontSize: 11))]),
          if ((n['t']??'').isNotEmpty) Text(n['t'], style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
          Text(n['x']??'', style: const TextStyle(color: Colors.grey, fontSize: 12)),
        ])),
      ])));
    });
  }

  Widget _folderTab() {
    if (folders.isEmpty) return const Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
      Icon(Icons.folder_open, size: 64, color: Colors.grey), SizedBox(height: 12),
      Text('No folders yet', style: TextStyle(color: Colors.grey)), Text('Tap + to create one', style: TextStyle(color: Colors.grey)),
    ]));
    return ListView.builder(padding: const EdgeInsets.all(8), itemCount: folders.length, itemBuilder: (_, i) {
      final f = folders[i];
      return Card(child: ListTile(
        leading: const Icon(Icons.folder, size: 36, color: Colors.indigo),
        title: Text(f.name, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text('${f.apps.length} apps'),
        onTap: () => _editFolder(f),
        trailing: IconButton(icon: const Icon(Icons.delete_outline, color: Colors.red), onPressed: () { setState(() => folders.removeWhere((x) => x.id == f.id)); _saveFolder(); }),
      ));
    });
  }

  Widget _photoTab() => Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
    const Icon(Icons.photo_library, size: 64, color: Colors.indigo),
    const SizedBox(height: 16),
    const Text('Photo Widget', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
    const SizedBox(height: 8),
    const Text('Photos change every 5 minutes\nTap photo to advance manually', textAlign: TextAlign.center, style: TextStyle(color: Colors.grey)),
    const SizedBox(height: 24),
    ElevatedButton.icon(onPressed: () => _ch.invokeMethod('pinPhoto'), icon: const Icon(Icons.add), label: const Text('Add to Home Screen')),
    const SizedBox(height: 12),
    ElevatedButton.icon(onPressed: () => _ch.invokeMethod('pickPhotos', {'wid': 0}), icon: const Icon(Icons.photo_album), label: const Text('Select Photos'), style: ElevatedButton.styleFrom(backgroundColor: Colors.indigo, foregroundColor: Colors.white)),
  ]));

  Widget _settingsTab() => ListView(padding: const EdgeInsets.all(14), children: [
    Card(child: Padding(padding: const EdgeInsets.all(14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Text('Notifications Widget Color', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
      const SizedBox(height: 8),
      _colorRow('Color & Opacity', nr, ng, nb, na, (r,g,b,a) { setState(() { nr=r; ng=g; nb=b; na=a; }); _saveNotif(); }),
    ]))),
    const SizedBox(height: 10),
    Card(child: Padding(padding: const EdgeInsets.all(14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Text('Folder Widget Color', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
      const SizedBox(height: 8),
      _colorRow('Color & Opacity', fr, fg, fb, fa, (r,g,b,a) { setState(() { fr=r; fg=g; fb=b; fa=a; }); _saveFolder(); }),
    ]))),
    const SizedBox(height: 10),
    Card(child: ListTile(leading: const Icon(Icons.apps, color: Colors.indigo), title: const Text('Notification Apps'), subtitle: Text(allowed.isEmpty ? 'All apps' : '${allowed.length} selected'), trailing: const Icon(Icons.chevron_right), onTap: _pickApps)),
  ]);

  Future<void> _editFolder([Folder? folder]) async {
    final result = await Navigator.push<Folder>(context, MaterialPageRoute(builder: (_) => FolderEdit(apps: apps, folder: folder)));
    if (result != null) {
      setState(() { if (folder==null) folders.add(result); else { final i=folders.indexWhere((f)=>f.id==folder.id); if (i>=0) folders[i]=result; } });
      await _saveFolder();
    }
  }
}

class FolderEdit extends StatefulWidget {
  final List<AppInfo> apps; final Folder? folder;
  const FolderEdit({super.key, required this.apps, this.folder});
  @override State<FolderEdit> createState() => _FolderEditState();
}

class _FolderEditState extends State<FolderEdit> {
  late TextEditingController _name;
  late Set<String> _sel;
  String _q = '';

  @override
  void initState() { super.initState(); _name = TextEditingController(text: widget.folder?.name ?? ''); _sel = Set<String>.from(widget.folder?.apps ?? []); }
  @override
  void dispose() { _name.dispose(); super.dispose(); }

  void _save() {
    final n = _name.text.trim();
    if (n.isEmpty) { ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enter folder name'))); return; }
    Navigator.pop(context, Folder(id: widget.folder?.id ?? '${DateTime.now().millisecondsSinceEpoch}', name: n, apps: _sel.toList()));
  }

  @override
  Widget build(BuildContext context) {
    final filtered = widget.apps.where((a) => a.name.toLowerCase().contains(_q.toLowerCase()) || a.pkg.toLowerCase().contains(_q.toLowerCase())).toList();
    return Scaffold(
      appBar: AppBar(title: Text(widget.folder==null?'New Folder':'Edit Folder'), actions: [TextButton.icon(onPressed: _save, icon: const Icon(Icons.check), label: const Text('Save'))]),
      body: Column(children: [
        Padding(padding: const EdgeInsets.fromLTRB(12,12,12,6), child: TextField(controller: _name, decoration: const InputDecoration(labelText: 'Folder name', border: OutlineInputBorder(), prefixIcon: Icon(Icons.folder)))),
        Padding(padding: const EdgeInsets.fromLTRB(12,6,12,6), child: TextField(decoration: const InputDecoration(hintText: 'Search...', prefixIcon: Icon(Icons.search), border: OutlineInputBorder()), onChanged: (v) => setState(() => _q=v))),
        Padding(padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4), child: Row(children: [
          Text('${_sel.length} selected', style: const TextStyle(color: Colors.indigo, fontWeight: FontWeight.w600)),
          const Spacer(),
          TextButton(onPressed: () => setState(() => _sel=widget.apps.map((a)=>a.pkg).toSet()), child: const Text('All')),
          TextButton(onPressed: () => setState(() => _sel.clear()), child: const Text('Clear')),
        ])),
        const Divider(height: 1),
        Expanded(child: ListView.builder(itemCount: filtered.length, itemBuilder: (_, i) {
          final a = filtered[i];
          return CheckboxListTile(title: Text(a.name), subtitle: Text(a.pkg, style: const TextStyle(fontSize: 10)), value: _sel.contains(a.pkg), onChanged: (v) => setState(() { if (v==true) _sel.add(a.pkg); else _sel.remove(a.pkg); }));
        })),
      ]),
    );
  }
}
