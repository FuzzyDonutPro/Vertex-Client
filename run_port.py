import os
import shutil

src_veinforge = r'C:\Users\jerem\.gemini\antigravity\brain\b37eaa5b-a964-4362-87cf-74b3aa143feb\scratch\VeinForge\src\main\java\me\grish\veinforge\macro\impl\mining\BlockMiner'
dest_vertex = r'C:\Users\jerem\CLionProjects\Vertex-Client\Vertex-Client-1.0.6\src\main\java\com\vertexai\macro\impl\mining\BlockMiner'

# 1. Copy BlockMiner
if os.path.exists(dest_vertex):
    shutil.rmtree(dest_vertex)
shutil.copytree(src_veinforge, dest_vertex)

# 2. Refactor imports and package names in all Java files in BlockMiner
for root, dirs, files in os.walk(dest_vertex):
    for f in files:
        if f.endswith('.java'):
            filepath = os.path.join(root, f)
            with open(filepath, 'r', encoding='utf-8') as file:
                content = file.read()
            
            # Refactor packages
            content = content.replace('me.grish.veinforge', 'com.vertexai')
            
            # Apply 3 block reach cap in BreakingState
            if f == 'BreakingState.java':
                content = content.replace('private static final double MAX_MINE_DISTANCE = 4;', 'private static final double MAX_MINE_DISTANCE = 3;')
            
            with open(filepath, 'w', encoding='utf-8') as file:
                file.write(content)

# 3. Register BlockMiner in FeatureManager
feature_manager_path = r'C:\Users\jerem\CLionProjects\Vertex-Client\Vertex-Client-1.0.6\src\main\java\com\vertexai\feature\FeatureManager.java'
if os.path.exists(feature_manager_path):
    with open(feature_manager_path, 'r', encoding='utf-8') as file:
        content = file.read()
    if 'BlockMiner.getInstance()' not in content:
        content = content.replace('public void init() {', 'public void init() {\n        features.add(com.vertexai.macro.impl.mining.BlockMiner.BlockMiner.getInstance());')
        with open(feature_manager_path, 'w', encoding='utf-8') as file:
            file.write(content)

print('Successfully ported BlockMiner, refactored packages, applied 3 block reach cap, and registered in FeatureManager.')
