package amymialee.peculiarpieces.component;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.NotNull;

public class WardingComponent implements AutoSyncedComponent {
    private final Chunk chunk;
    private final IntSet set = new IntOpenHashSet();
    private byte[] syncCache;

    public WardingComponent(Chunk chunk) {
        this.chunk = chunk;
    }

    public boolean getWard(WorldView world, BlockPos pos) {
        var block = world.getBlockState(pos);
        return !block.isAir() && !(block.isOf(Blocks.PISTON) || block.isOf(Blocks.STICKY_PISTON) || block.isOf(Blocks.MOVING_PISTON) || block.isOf(Blocks.PISTON_HEAD)) && this.getWard(pos);
    }

    private int pack(BlockPos pos) {
        int i = 0;
        i |= (pos.getX()&0xF)<<20;
        i |= (pos.getZ()&0xF)<<16;
        // cast to short for sign extension
        i |= ((short)pos.getY())&0xFFFF;
        return i;
    }
    
    public boolean getWard(BlockPos pos) {
        return this.set.contains(pack(pos));
    }

    public void setWard(BlockPos pos, boolean warded) {
        if (warded) {
            this.set.add(pack(pos));
        } else {
            this.set.remove(pack(pos));
        }
        syncCache = null;
        this.chunk.setNeedsSaving(true);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag) {
        this.set.clear();
        if (tag.contains("positions", NbtElement.LIST_TYPE)) {
            // old format
            var list = tag.getList("positions", NbtElement.COMPOUND_TYPE);
            for (var element : list) {
                if (element instanceof NbtCompound compound) {
                    var pos = NbtHelper.toBlockPos(compound);
                    this.set.add(pack(pos));
                }
            }
        } else {
            var buf = Unpooled.wrappedBuffer(tag.getByteArray("P"));
            while (buf.isReadable(3)) {
                set.add(buf.readMedium()&0xFFFFFF);
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag) {
        byte[] bys = syncCache;
        if (bys == null) {
            var buf = Unpooled.buffer(set.size()*3);
            var iter = set.intIterator();
            while (iter.hasNext()) {
                buf.writeMedium(iter.nextInt());
            }
            syncCache = bys = buf.array();
        }
        if (bys.length > 0) {
            tag.putByteArray("P", bys);
        }
    }
}