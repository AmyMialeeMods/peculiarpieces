package amymialee.peculiarpieces.screens;

import amymialee.peculiarpieces.PeculiarPieces;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class RedstoneTriggerScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    public RedstoneTriggerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2));
    }

    public RedstoneTriggerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(PeculiarPieces.REDSTONE_TRIGGER_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        for(var j = 0; j < 2; ++j) {
            this.addSlot(new Slot(inventory, j, 71 + (j * 18), 20) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return stack.getNbt() != null && stack.getNbt().contains("pp:target");
                }
            });
        }
        for(var j = 0; j < 3; ++j) {
            for(var k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + j * 9 + 9, 8 + k * 18, j * 18 + 51));
            }
        }
        for(var j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 109));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        var copy = ItemStack.EMPTY;
        var slot = this.slots.get(index);
        if (slot.hasStack()) {
            var itemStack = slot.getStack();
            copy = itemStack.copy();
            if (index < 2 ? !this.insertItem(itemStack, 2, this.slots.size(), true) : !this.insertItem(itemStack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
            if (itemStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return copy;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}