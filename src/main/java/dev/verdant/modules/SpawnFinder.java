package dev.verdant.modules;

import dev.verdant.render.EspTarget;
import dev.verdant.setting.BoolSetting;
import dev.verdant.setting.ColorSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

public class SpawnFinder extends EspModule {
    public final BoolSetting mobSpawners = add(new BoolSetting("Mob Spawners", true));
    public final ColorSetting spawnerColor = add(new ColorSetting("Spawner Color", 0x3DDCFF));
    public final BoolSetting trialSpawners = add(new BoolSetting("Trial Spawners", true));
    public final ColorSetting trialColor = add(new ColorSetting("Trial Color", 0xFFC24A));
    public final BoolSetting highlightNearest = add(new BoolSetting("Highlight Nearest", true));
    public final ColorSetting nearestColor = add(new ColorSetting("Nearest Color", 0xFF4D6D));
    public final BoolSetting showRange = add(new BoolSetting("Show Activation Range", false));
    public final ColorSetting rangeColor = add(new ColorSetting("Range Color", 0xFF5A5A));

    public SpawnFinder() {
        super("Spawn Finder", "Highlights mob spawners and trial spawners");
        spawnerColor.visibleWhen(mobSpawners::get);
        trialColor.visibleWhen(trialSpawners::get);
        nearestColor.visibleWhen(highlightNearest::get);
        rangeColor.visibleWhen(showRange::get);
        rangeColor.alpha = 0.6f;
        esp = new EspSettings(this, true);
        add(scanInterval);
    }

    @Override
    protected List<EspTarget> scan(Minecraft mc, ClientLevel level) {
        List<EspTarget> out = new ArrayList<>();
        double range = esp.range.get() + 16.0;
        double rangeSq = range * range;
        int px = mc.player.getBlockX();
        int py = mc.player.getBlockY();
        int pz = mc.player.getBlockZ();
        int pcx = px >> 4;
        int pcz = pz >> 4;
        int rc = (int) Math.ceil(range / 16.0);

        BlockPos nearest = null;
        double nearestSq = Double.MAX_VALUE;
        List<BlockPos> mobs = new ArrayList<>();
        List<BlockPos> trials = new ArrayList<>();

        for (int cx = pcx - rc; cx <= pcx + rc; cx++) {
            for (int cz = pcz - rc; cz <= pcz + rc; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                if (chunk == null || chunk.getBlockEntities().isEmpty()) continue;
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    BlockPos pos = be.getBlockPos();
                    double dx = pos.getX() + 0.5 - px, dy = pos.getY() + 0.5 - py, dz = pos.getZ() + 0.5 - pz;
                    double d = dx * dx + dy * dy + dz * dz;
                    if (d > rangeSq) continue;
                    if (be instanceof SpawnerBlockEntity && mobSpawners.get()) {
                        mobs.add(pos);
                        if (d < nearestSq) { nearestSq = d; nearest = pos; }
                    } else if (be instanceof TrialSpawnerBlockEntity && trialSpawners.get()) {
                        trials.add(pos);
                    }
                }
            }
        }

        for (BlockPos pos : mobs) {
            AABB box = new AABB(pos);
            if (highlightNearest.get() && pos.equals(nearest)) out.add(EspTarget.of(box, nearestColor));
            else out.add(EspTarget.of(box, spawnerColor));
            if (showRange.get()) {
                AABB r = new AABB(pos).inflate(16.0);
                out.add(new EspTarget(r, rangeColor, 0xFFFFFF, true));
            }
        }
        for (BlockPos pos : trials) {
            out.add(EspTarget.of(new AABB(pos), trialColor));
        }
        return out;
    }
}
