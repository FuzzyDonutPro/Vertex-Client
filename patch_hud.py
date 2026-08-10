import os

hud_path = r'C:\Users\jerem\CLionProjects\Vertex-Client\Vertex-Client-1.0.6\src\main\java\com\vertexai\config\Categorie\HUD.java'

with open(hud_path, 'r', encoding='utf-8') as f:
    hud = f.read()

if 'public boolean enableMiningHud = true;' not in hud:
    hud = hud.replace('public boolean enableSpotifyHud = true;', 'public boolean enableSpotifyHud = true;\n\n    @Expose\n    @ConfigOption(\n            name = "Mining HUD",\n            desc = "Enable the mining overlay"\n    )\n    public boolean enableMiningHud = true;')
    
    with open(hud_path, 'w', encoding='utf-8') as f:
        f.write(hud)

print('Added enableMiningHud to HUD.java')
