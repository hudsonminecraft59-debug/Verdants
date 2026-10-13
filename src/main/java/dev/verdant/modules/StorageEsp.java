package dev.verdant.modules;

import dev.verdant.render.EspTarget;
import dev.verdant.setting.BoolSetting;
import dev.verdant.setting.ColorSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StorageEsp extends EspModule {
    public final BoolSetting chests = add(new BoolSetting("Chests", true));
    public final ColorSetting chestColor = add(new ColorSetting("Chest Color", 0xF2A93B));
    public final BoolSetting trapped = add(new BoolSetting("Trapped Chests", true));
    public final ColorSetting trappedColor = add(new ColorSetting("Trapped Color", 0xE5483F));
    public final BoolSetting ender = add(new BoolSetting("Ender Chests", true));
    public final ColorSetting enderColor = add(new ColorSetting("Ender Color", 0xB35CFF));
    public final BoolSetting barrels = add(new BoolSetting("Barrels", false));
    public final ColorSetting barrelColor = add(new ColorSetting("Barrel Color", 0xC98B52));
    public final BoolSetting hoppers = add(new BoolSetting("Hoppers", true));
    public final ColorSetting hopperColor = add(new ColorSetting("Hopper Color", 0x7FD1F5));
    public final BoolSetting shulkers = add(new BoolSetting("Shulker Boxes", true));
    public final BoolSetting shulkerDye = add(new BoolSetting("Use Shulker Dye Color", true));
    public final ColorSetting shulkerColor = add(new ColorSetting("Shulker Color", 0xD64DFF));
    public final BoolSetting mergeDouble = add(new BoolSetting("Merge Double Chests", true));

    public StorageEsp() {
        super("Storage ESP", "Highlights chests, hoppers and shulker boxes");
        chestColor.visibleWhen(chests::get);
        trappedColor.visibleWhen(trapped::get);
        enderColor.visibleWhen(ender::get);
        barrelColor.visibleWhen(barrels::get);
        hopperColor.visibleWhen(hoppers::get);
        shulkerDye.visibleWhen(shulkers::get);
        shulkerColor.visibleWhen(() -> shulkers.get() && !shulkerDye.get());
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

        for (int cx = pcx - rc; cx <= pcx + rc; cx++) {
            for (int cz = pcz - rc; cz <= pcz + rc; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                if (chunk == null || chunk.getBlockEntities().isEmpty()) continue;
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    BlockPos pos = be.getBlockPos();
                    double dx = pos.getX() + 0.5 - px, dy = pos.getY() + 0.5 - py, dz = pos.getZ() + 0.5 - pz;
                    if (dx * dx + dy * dy + dz * dz > rangeSq) continue;
                    classify(level, be, pos, out);
                }
            }
        }
        return out;
    }

    private void classify(ClientLevel level, BlockEntity be, BlockPos pos, List<EspTarget> out) {
        BlockState state = be.getBlockState();
        if (be instanceof TrappedChestBlockEntity) {
            if (trapped.get()) addChest(level, pos, state, trappedColor, out);
        } else if (be instanceof ChestBlockEntity) {
            if (chests.get()) addChest(level, pos, state, chestColor, out);
        } else if (be instanceof EnderChestBlockEntity) {
            if (ender.get()) out.add(EspTarget.of(boxOf(level, pos, state), enderColor));
        } else if (be instanceof BarrelBlockEntity) {
            if (barrels.get()) out.add(EspTarget.of(boxOf(level, pos, state), barrelColor));
        } else if (be instanceof HopperBlockEntity) {
            if (hoppers.get()) out.add(EspTarget.of(boxOf(level, pos, state), hopperColor));
        } else if (be instanceof ShulkerBoxBlockEntity) {
            if (!shulkers.get()) return;
            AABB box = boxOf(level, pos, state);
            DyeColor dye = state.getBlock() instanceof ShulkerBoxBlock sb ? sb.getColor() : null;
            if (shulkerDye.get() && dye != null) out.add(EspTarget.fixed(box, dye.getFireworkColor()));
            else out.add(EspTarget.of(box, shulkerColor));
        }
    }

    private void addChest(ClientLevel level, BlockPos pos, BlockState state, ColorSetting color, List<EspTarget> out) {
        AABB box = boxOf(level, pos, state);
        if (mergeDouble.get() && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction dir = ChestBlock.getConnectedDirection(state);
            BlockPos other = pos.relative(dir);
            if (level.getBlockEntity(other) instanceof ChestBlockEntity) {
                if (pos.asLong() > other.asLong()) return; // the partner draws the merged box
                AABB otherBox = boxOf(level, other, level.getBlockState(other));
                box = box.minmax(otherBox);
            }
        }
        out.add(EspTarget.of(box, color));
    }

    private static AABB boxOf(ClientLevel level, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getShape(level, pos);
        AABB b = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
        return b.move(pos);
    }
}
