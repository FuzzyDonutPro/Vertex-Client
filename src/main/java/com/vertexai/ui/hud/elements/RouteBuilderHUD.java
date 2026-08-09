package com.vertexai.ui.hud.elements;

import lombok.Getter;
import com.vertexai.Vertex;
import com.vertexai.client.overlay.TextHud;
import com.vertexai.macro.impl.navigation.RouteBuilder;
import com.vertexai.handler.GraphHandler;
import com.vertexai.handler.RouteHandler;
import com.vertexai.ui.hud.ColorPalette;
import com.vertexai.util.KeyPressUtil;
import com.vertexai.util.PlayerUtil;
import com.vertexai.util.helper.graph.Graph;
import com.vertexai.util.helper.route.Route;
import com.vertexai.util.helper.route.RouteWaypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class RouteBuilderHUD extends TextHud {
   private static final RouteBuilderHUD instance = new RouteBuilderHUD();
   private final RouteBuilder routeBuilder = RouteBuilder.getInstance();
   private final GraphHandler graphHandler = GraphHandler.instance;
   private final RouteHandler routeHandler = RouteHandler.getInstance();
   public RouteBuilderHUD() {
      super();
      this.x = 5;
      this.y = 90;
      this.enabled = true;
   }

   public static RouteBuilderHUD getInstance() {
      return instance;
   }

   @Override
   protected int getAccentColor() {
      return ColorPalette.VIOLET_400;
   }

   @Override
   protected boolean shouldShow() {
      if (!super.shouldShow()) {
         return false;
      }
      return routeBuilder.isRunning() || graphHandler.isEditing() || graphHandler.isDebugRenderEnabled();
   }

   @Override
   protected void getLines(List<String> lines, boolean example) {
      if (example) {
         lines.add("Â§dÂ§lROUTE BUILDER");
         lines.add("Â§8Â§m------------------------");
         lines.add("Â§8Â» Â§7Status: Â§aACTIVE");
         lines.add("Â§8Â» Â§7Mode: Â§fETHERWARP");
         lines.add("Â§8Â» Â§7Nodes: Â§a15");
         lines.add("Â§8Â» Â§aStand: Â§f-3, 110, 15");
         lines.add("Â§8Â» Â§cSelect: Â§f-4, 110, 16 Â§8(Â§f2.4mÂ§8)");
         lines.add("Â§8Â» Â§bHover: Â§f-2, 110, 14 Â§8(Â§f6.1mÂ§8)");
         lines.add("Â§8Â§m------------------------");
         lines.add("Â§8Â» Â§7Save: Â§f/graph save");
         return;
      }

      boolean routeEditing = routeBuilder.isRunning();
      if (routeEditing && !graphHandler.isEditing() && !graphHandler.isDebugRenderEnabled()) {
         addRouteEditorLines(lines);
         return;
      }

      boolean editing = graphHandler.isEditing();
      boolean debugRender = graphHandler.isDebugRenderEnabled();
      Graph<RouteWaypoint> graph = graphHandler.getCachedGraph();
      if (graph == null) {
         graph = graphHandler.getActiveGraph();
      }

      BlockPos standingBlock = graphHandler.getCachedStandingBlock();
      BlockPos hoveredBlock = graphHandler.getCachedHoveredBlock();
      RouteWaypoint hoveredWaypoint = graphHandler.getCachedHoveredWaypoint();
      RouteWaypoint selected = graphHandler.getLastPos();

      lines.add("Â§dÂ§lROUTE BUILDER");
      lines.add("Â§8Â§m------------------------");
      lines.add("Â§8Â» Â§7Status: " + (editing ? "Â§aEDITING" : "Â§fVIEWING"));
      
      String shownGraph = editing ? graphHandler.getActiveGraphKey() : (debugRender ? graphHandler.getDebugGraphKey() : graphHandler.getActiveGraphKey());
      lines.add("Â§8Â» Â§7Graph: Â§f" + shownGraph + " " + (graphHandler.isDirty() ? " Â§8(Â§cDirtyÂ§8)" : ""));
      if (editing) {
         lines.add("Â§8Â» Â§7Placement: Â§b" + graphHandler.getEditorPlacementType());
      }
      lines.add("Â§8Â» Â§7Total Nodes: Â§a" + graph.map.size());

      if (standingBlock != null) {
         lines.add("Â§8Â» Â§aStanding: Â§f" + standingBlock.getX() + ", " + standingBlock.getY() + ", " + standingBlock.getZ());
      }
      
      if (selected != null) {
         String dist = formatDistanceTo(selected.toBlockPos());
         lines.add("Â§8Â» Â§cSelected: Â§f" + selected.getX() + ", " + selected.getY() + ", " + selected.getZ() + (dist == null ? "" : " Â§8(Â§f" + dist + "Â§8)"));
      }
      
      if (hoveredBlock != null) {
         String dist = formatDistanceTo(hoveredBlock);
         lines.add("Â§8Â» Â§bHovered: Â§f" + hoveredBlock.getX() + ", " + hoveredBlock.getY() + ", " + hoveredBlock.getZ() + (dist == null ? "" : " Â§8(Â§f" + dist + "Â§8)"));
      }

      RouteWaypoint infoWaypoint = hoveredWaypoint != null ? hoveredWaypoint : selected;
      if (infoWaypoint != null) {
         Set<RouteWaypoint> edges = graph.map.getOrDefault(infoWaypoint, Collections.emptySet());
         int outgoing = edges.size();
         int incoming = graphHandler.getIncomingEdgeCount(graph, infoWaypoint);
         lines.add("Â§8Â§m------------------------");
         lines.add("Â§8Â» Â§7Node Method: Â§f" + infoWaypoint.getTransportMethod());
         lines.add("Â§8Â» Â§7Connections: Â§7Out:Â§f" + outgoing + " Â§7In:Â§f" + incoming);
      }

      lines.add("Â§8Â§m------------------------");
      for (String hint : graphHandler.getEditorControlHints()) {
         lines.add("Â§8Â» Â§7" + hint.replace(": ", ": Â§f"));
      }
      lines.add("Â§8Â» Â§7Save: Â§f/graph save");
   }

   private void addRouteEditorLines(List<String> lines) {
      lines.add("Â§dÂ§lROUTE EDITOR");
      lines.add("Â§8Â§m------------------------");
      lines.add("Â§8Â» Â§7Status: Â§aACTIVE");

      Route selectedRoute = routeHandler.getSelectedRoute();
      String selectedRouteName = routeHandler.getSelectedRouteName();
      int waypointCount = selectedRoute == null ? 0 : selectedRoute.size();
      lines.add("Â§8Â» Â§7Route: Â§f" + selectedRouteName);
      lines.add("Â§8Â» Â§7Waypoints: Â§a" + waypointCount);

      BlockPos standingBlock = PlayerUtil.getBlockStandingOn();
      if (standingBlock != null) {
         lines.add("Â§8Â» Â§aStanding: Â§f" + standingBlock.getX() + ", " + standingBlock.getY() + ", " + standingBlock.getZ());
      }

      if (selectedRoute != null && !selectedRoute.isEmpty() && standingBlock != null) {
         Optional<RouteWaypoint> closest = selectedRoute.getClosest(standingBlock);
         if (closest.isPresent()) {
            RouteWaypoint waypoint = closest.get();
            int index = selectedRoute.indexOf(waypoint) + 1;
            String dist = formatDistanceTo(waypoint.toBlockPos());
            lines.add("Â§8Â» Â§bClosest: Â§f#" + index + " Â§7(" + waypoint.getTransportMethod() + ") Â§f" + waypoint.getX() + ", " + waypoint.getY() + ", " + waypoint.getZ() + (dist == null ? "" : " Â§8(Â§f" + dist + "Â§8)"));
         }
      }

      lines.add("Â§8Â§m------------------------");
      var config = Vertex.config();
      if (config != null) {
         lines.add("Â§8Â» Â§7Add WALK: Â§f[" + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderWalkAddKeybind) + "]");
         lines.add("Â§8Â» Â§7Add ETHER: Â§f[" + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderEtherwarpAddKeybind) + "]");
         lines.add("Â§8Â» Â§7Remove: Â§f[" + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderRemoveKeybind) + "]");
      }
      lines.add("Â§8Â» Â§7Help: Â§f/rb keys");
   }

   private String formatOutgoingPreview(Set<RouteWaypoint> edges, int max) {
      if (edges == null || edges.isEmpty() || max <= 0) {
         return null;
      }

      List<RouteWaypoint> list = new ArrayList<>(edges);
      list.sort(
              Comparator.comparingInt(RouteWaypoint::getX)
                      .thenComparingInt(RouteWaypoint::getY)
                      .thenComparingInt(RouteWaypoint::getZ)
      );

      StringBuilder sb = new StringBuilder();
      int shown = Math.min(max, list.size());
      for (int i = 0; i < shown; i++) {
         RouteWaypoint wp = list.get(i);
         if (i > 0) {
            sb.append("Â§8, ");
         }
         sb.append("Â§f");
         sb.append(wp.getX()).append(",").append(wp.getY()).append(",").append(wp.getZ());
      }
      if (list.size() > shown) {
         sb.append(" Â§8+").append(list.size() - shown);
      }
      return sb.toString();
   }

   private String formatDistanceTo(BlockPos pos) {
      if (pos == null || mc.player == null) {
         return null;
      }
      Vec3 player = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      double d = player.distanceTo(target);
      return String.format("%.1fm", d);
   }
}
