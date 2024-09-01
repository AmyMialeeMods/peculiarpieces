package amymialee.peculiarpieces;

import amymialee.peculiarpieces.blocks.FlagBlock;
import amymialee.peculiarpieces.callbacks.PlayerCrouchCallback;
import amymialee.peculiarpieces.callbacks.PlayerCrouchConsumingBlock;
import amymialee.peculiarpieces.callbacks.PlayerJumpCallback;
import amymialee.peculiarpieces.callbacks.PlayerJumpConsumingBlock;
import amymialee.peculiarpieces.component.PeculiarComponentInitializer;
import amymialee.peculiarpieces.component.WardingComponent;
import amymialee.peculiarpieces.effects.FlightStatusEffect;
import amymialee.peculiarpieces.effects.OpenStatusEffect;
import amymialee.peculiarpieces.items.TorchQuiverItem;
import amymialee.peculiarpieces.recipe.ShapedNbtRecipe;
import amymialee.peculiarpieces.recipe.ShapelessNbtRecipe;
import amymialee.peculiarpieces.registry.PeculiarBlocks;
import amymialee.peculiarpieces.registry.PeculiarEntities;
import amymialee.peculiarpieces.registry.PeculiarItems;
import amymialee.peculiarpieces.screens.CouriporterScreenHandler;
import amymialee.peculiarpieces.screens.CreativeBarrelScreenHandler;
import amymialee.peculiarpieces.screens.EquipmentStandScreenHandler;
import amymialee.peculiarpieces.screens.FishTankScreenHandler;
import amymialee.peculiarpieces.screens.PackedPouchScreenHandler;
import amymialee.peculiarpieces.screens.PedestalScreenHandler;
import amymialee.peculiarpieces.screens.PotionPadScreenHandler;
import amymialee.peculiarpieces.screens.RedstoneTriggerScreenHandler;
import amymialee.peculiarpieces.screens.WarpScreenHandler;
import amymialee.peculiarpieces.util.ExtraPlayerDataWrapper;
import amymialee.peculiarpieces.util.RedstoneManager;
import amymialee.peculiarpieces.util.WarpManager;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroup.DisplayContext;
import net.minecraft.item.ItemGroup.Entries;
import net.minecraft.item.ItemGroup.EntryCollector;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

@SuppressWarnings("unused")
public class PeculiarPieces implements ModInitializer {
    public static final String MOD_ID = "peculiarpieces";
    //ItemGroups
    public static final ItemGroup PIECES_GROUP = FabricItemGroup.builder().displayName(Text.translatable("itemGroup.peculiarpieces.peculiarpieces_group")).icon(PeculiarItems::getPeculiarIcon).entries(PeculiarItemGroups::buildPieces).build();
    public static final ItemGroup CREATIVE_GROUP = FabricItemGroup.builder().displayName(Text.translatable("itemGroup.peculiarpieces.peculiarpieces_creative_group")).icon(PeculiarItems::getCreativeIcon).entries(PeculiarItemGroups::buildCreative).build();
    public static final ItemGroup POTION_GROUP = FabricItemGroup.builder().displayName(Text.translatable("itemGroup.peculiarpieces.peculiarpieces_potion_group")).icon(PeculiarItems::getPotionIcon).entries(PeculiarItemGroups::buildPotion).build();
    //ScreenHandlers
    
    public static final ScreenHandlerType<WarpScreenHandler> WARP_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(id("warp_block"), WarpScreenHandler::new);
    public static final ScreenHandlerType<CouriporterScreenHandler> COURIPORTER_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(id("couriporter"), CouriporterScreenHandler::new);
    public static final ScreenHandlerType<PotionPadScreenHandler> POTION_PAD_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(id("potion_pad"), PotionPadScreenHandler::new);
    public static final ScreenHandlerType<PackedPouchScreenHandler> BUSTLING_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(id("bustling_bundle"), (a, b) -> new PackedPouchScreenHandler(a, b, PeculiarItems.PACKED_POUCH.getDefaultStack().copy()));
    public static final ScreenHandlerType<PedestalScreenHandler> PEDESTAL_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(id("pedestal"), PedestalScreenHandler::new);
    public static final ScreenHandlerType<FishTankScreenHandler> FISH_TANK_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(id("fish_tank"), FishTankScreenHandler::new);
    public static final ScreenHandlerType<RedstoneTriggerScreenHandler> REDSTONE_TRIGGER_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(id("redstone_trigger"), RedstoneTriggerScreenHandler::new);
    public static final ScreenHandlerType<CreativeBarrelScreenHandler> CREATIVE_BARREL_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(id("creative_barrel"), CreativeBarrelScreenHandler::new);
    public static final ScreenHandlerType<EquipmentStandScreenHandler> EQUIPMENT_STAND_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(id("equipment_stand"), EquipmentStandScreenHandler::new);
    //Tags
    public static final TagKey<EntityType<?>> MOUNT_BLACKLIST = TagKey.of(Registries.ENTITY_TYPE.getKey(), id("mount_blacklist"));
    public static final TagKey<EntityType<?>> UNGRABBABLE = TagKey.of(Registries.ENTITY_TYPE.getKey(), id("ungrabbable"));
    public static final TagKey<Block> WARP_BINDABLE = TagKey.of(Registries.BLOCK.getKey(), id("warp_bindable"));
    public static final TagKey<Block> SCAFFOLDING = TagKey.of(Registries.BLOCK.getKey(), id("scaffolding"));
    public static final TagKey<Block> SHEARS_MINEABLE = TagKey.of(Registries.BLOCK.getKey(), id("mineable/shears"));
    public static final TagKey<Item> BARRIERS = TagKey.of(Registries.ITEM.getKey(), id("barriers"));
    public static final TagKey<Item> TORCHES = TagKey.of(Registries.ITEM.getKey(), id("torches"));
    public static final TagKey<Item> TOTEMS = TagKey.of(Registries.ITEM.getKey(), id("totems"));
    //Flight
    public static final StatusEffect FLIGHT_EFFECT = Registry.register(Registries.STATUS_EFFECT, id("flight"), new FlightStatusEffect(StatusEffectCategory.BENEFICIAL, 6670591));
    public static final Potion FLIGHT = Registry.register(Registries.POTION, id("flight"), new Potion(new StatusEffectInstance(FLIGHT_EFFECT, 3600)));
    public static final Potion LONG_FLIGHT = Registry.register(Registries.POTION, id("long_flight"), new Potion("flight", new StatusEffectInstance(FLIGHT_EFFECT, 9600)));
    //Glowing
    public static final Potion GLOWING = Registry.register(Registries.POTION, id("glowing"), new Potion(new StatusEffectInstance(StatusEffects.GLOWING, 3600)));
    public static final Potion LONG_GLOWING = Registry.register(Registries.POTION, id("long_glowing"), new Potion("glowing", new StatusEffectInstance(StatusEffects.GLOWING, 9600)));
    //Concealment
    public static final StatusEffect CONCEALMENT_EFFECT = Registry.register(Registries.STATUS_EFFECT, id("concealment"), new OpenStatusEffect(StatusEffectCategory.BENEFICIAL, 8356754));
    public static final Potion CONCEALMENT = Registry.register(Registries.POTION, id("concealment"), new Potion(new StatusEffectInstance(CONCEALMENT_EFFECT, 3600)));
    public static final Potion LONG_CONCEALMENT = Registry.register(Registries.POTION, id("long_concealment"), new Potion("concealment", new StatusEffectInstance(CONCEALMENT_EFFECT, 9600)));
    //Impervious
    public static final StatusEffect INVULNERABILITY_EFFECT = Registry.register(Registries.STATUS_EFFECT, id("invulnerability"), new OpenStatusEffect(StatusEffectCategory.BENEFICIAL, 16772864));
    public static final Potion INVULNERABILITY = Registry.register(Registries.POTION, id("invulnerability"), new Potion(new StatusEffectInstance(INVULNERABILITY_EFFECT, 3600)));
    public static final Potion LONG_INVULNERABILITY = Registry.register(Registries.POTION, id("long_invulnerability"), new Potion("invulnerability", new StatusEffectInstance(INVULNERABILITY_EFFECT, 9600)));
    //Gamerules
    public static final GameRules.Key<GameRules.BooleanRule> DO_EXPLOSIONS_BREAK = GameRuleRegistry.register("pp:explosionsBreakBlocks", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> DO_EXPLOSIONS_ALWAYS_DROP = GameRuleRegistry.register("pp:explosionsAlwaysDrop", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> NO_MOB_PUSHING = GameRuleRegistry.register("pp:doEntityPush", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
    //SoundEvents
    public static final SoundEvent ENTITY_SHEEP_YIPPEE = Registry.register(Registries.SOUND_EVENT, "peculiarpieces.sheep.yippee", SoundEvent.of(id("peculiarpieces.sheep.yippee")));
    public static final SoundEvent ENTITY_SHEEP_YIPPEE_ENGINEER = Registry.register(Registries.SOUND_EVENT, "peculiarpieces.sheep.yippee_engineer", SoundEvent.of(id("peculiarpieces.sheep.yippee_engineer")));
    //Particles
    public static final DefaultParticleType WARDING_AURA = FabricParticleTypes.simple();

    //RecipeSerializers
    public static final RecipeSerializer<ShapedRecipe> SHAPED_NBT_CRAFTING_SERIALZIER = RecipeSerializer.register(id("nbt_crafting_shaped").toString(), new ShapedNbtRecipe.Serializer());
    public static final RecipeSerializer<ShapelessRecipe> SHAPELESS_NBT_CRAFTING_SERIALZIER = RecipeSerializer.register(id("nbt_crafting_shapeless").toString(), new ShapelessNbtRecipe.Serializer());
    
    @Override
    public void onInitialize() {
        PeculiarItems.init();
        PeculiarBlocks.init();
        PeculiarEntities.init();
        Registry.register(Registries.PARTICLE_TYPE, PeculiarPieces.id("warding_aura"), WARDING_AURA);
        Registry.register(Registries.ITEM_GROUP, PeculiarPieces.id("peculiarpieces_group"), PIECES_GROUP);
        Registry.register(Registries.ITEM_GROUP, PeculiarPieces.id("peculiarpieces_creative_group"), CREATIVE_GROUP);
        Registry.register(Registries.ITEM_GROUP, PeculiarPieces.id("peculiarpieces_potion_group"), POTION_GROUP);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var literalArgumentBuilder = CommandManager.literal("peculiar").requires(source -> source.hasPermissionLevel(2));
            for (var gameMode : GameMode.values()) {
                literalArgumentBuilder
                        .then(CommandManager.literal("tempgamemode")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .then((CommandManager.literal(gameMode.getName())
                                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            var targets = EntityArgumentType.getPlayers(ctx, "targets");
                                                            var ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                                            for (var target : targets) {
                                                                if (target instanceof ExtraPlayerDataWrapper wrapper) {
                                                                    if (gameMode == wrapper.getStoredGameMode()) {
                                                                        continue;
                                                                    }
                                                                    var playerMode = target.interactionManager.getGameMode();
                                                                    if (playerMode != gameMode) {
                                                                        if (wrapper.getGameModeDuration() == 0) {
                                                                            wrapper.setStoredGameMode(playerMode);
                                                                        }
                                                                        target.changeGameMode(gameMode);
                                                                        wrapper.setGameModeDuration(ticks);
                                                                    } else if (wrapper.getGameModeDuration() > 0) {
                                                                        wrapper.setGameModeDuration(ticks);
                                                                    }
                                                                }
                                                            }
                                                            if (targets.size() == 1) {
                                                                ctx.getSource().sendFeedback(() -> Text.translatable("peculiar.commands.gamemode.success.single", gameMode.getName(), targets.iterator().next().getDisplayName()), true);
                                                            } else {
                                                                ctx.getSource().sendFeedback(() -> Text.translatable("peculiar.commands.gamemode.success.multiple", gameMode.getName(), targets.size()), true);
                                                            }
                                                            return 0;
                                                        }))))));
            }
            literalArgumentBuilder
                    .then(CommandManager.literal("checkpoint")
                            .then(CommandManager.argument("targets", EntityArgumentType.players())
                                    .then(CommandManager.argument("location", Vec3ArgumentType.vec3())
                                            .executes(ctx -> {
                                                var targets = EntityArgumentType.getPlayers(ctx, "targets");
                                                var pos = Vec3ArgumentType.getPosArgument(ctx, "location").toAbsolutePos(ctx.getSource());
                                                for (var target : targets) {
                                                    if (target instanceof ExtraPlayerDataWrapper wrapper) {
                                                        wrapper.setCheckpointPos(pos);
                                                        wrapper.setCheckpointWorld(ctx.getSource().getWorld().getRegistryKey());
                                                    }
                                                }
                                                if (targets.size() == 1) {
                                                    ctx.getSource().sendFeedback(() -> Text.translatable("peculiar.commands.checkpoint.success.single", pos.getX(), pos.getY(), pos.getZ(), targets.iterator().next().getDisplayName()), true);
                                                } else {
                                                    ctx.getSource().sendFeedback(() -> Text.translatable("peculiar.commands.checkpoint.success.multiple", pos.getX(), pos.getY(), pos.getZ(), targets.size()), true);
                                                }
                                                return 0;
                                            }))));
            literalArgumentBuilder
                    .then(CommandManager.literal("wardarea")
                            .then(CommandManager.argument("set", BoolArgumentType.bool())
                                    .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                            .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                                    .executes(context -> {
                                                        return ward(context, null);
                                                    })
                                                    .then(CommandManager.argument("filter", BlockStateArgumentType.blockState(access))
                                                            .executes(context -> {
                                                                return ward(context, BlockStateArgumentType.getBlockState(context, "filter").getBlockState());
                                                            }))))));
            literalArgumentBuilder
                    .then(CommandManager.literal("ward")
                            .then(CommandManager.argument("set", BoolArgumentType.bool())
                                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                            .executes(context -> {
                                                var source = context.getSource();
                                                var serverWorld = source.getWorld();
                                                var pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
                                                var chunk = serverWorld.getChunk(pos);
                                                var component = PeculiarComponentInitializer.WARDING.maybeGet(chunk);
                                                var ward = BoolArgumentType.getBool(context, "set");
                                                if (component.isPresent()) {
                                                    var wardingComponent = component.get();
                                                    wardingComponent.setWard(pos, ward);
                                                    PeculiarComponentInitializer.WARDING.sync(chunk);
                                                } else {
                                                    source.sendFeedback(() -> Text.translatable("peculiar.commands.ward.failure"), true);
                                                }
                                                source.sendFeedback(() -> Text.translatable("peculiar.commands.ward.success", ward ? "Warded" : "Unwarded", pos.getX(), pos.getY(), pos.getZ()), true);
                                                return 0;
                                            }))));
            literalArgumentBuilder
                    .then(CommandManager.literal("discard")
                            .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                    .executes(context -> {
                                        var targets = EntityArgumentType.getEntities(context, "targets");
                                        var total = 0;
                                        for (Entity entity : targets) {
                                            if (!(entity instanceof PlayerEntity)) {
                                                entity.discard();
                                                total++;
                                            }
                                        }
                                        if (targets.size() == 1) {
                                            var first = targets.iterator().next();
                                            if (first instanceof PlayerEntity) {
                                                context.getSource().sendFeedback(() -> Text.translatable("peculiar.commands.discard.failure.single"), true);
                                            } else {
                                                context.getSource().sendFeedback(() -> Text.translatable("peculiar.commands.discard.success.single", first.getDisplayName()), true);
                                            }
                                        } else {
                                            var ftotal = total;
                                            context.getSource().sendFeedback(() -> Text.translatable("peculiar.commands.discard.success.multiple", ftotal), true);
                                        }
                                        return targets.size();
                                    })));
            dispatcher.register(literalArgumentBuilder);
        });
        ServerTickEvents.END_WORLD_TICK.register(serverWorld -> WarpManager.tick());
        ServerTickEvents.END_WORLD_TICK.register(serverWorld -> RedstoneManager.tick());
        PlayerCrouchCallback.EVENT.register((player, world) -> {
            var pos = player.getBlockPos().add(0, -1, 0);
            var state = world.getBlockState(pos);
            if (state.getBlock() instanceof PlayerCrouchConsumingBlock block) {
                block.onCrouch(state, world, pos, player);
            }
        });
        PlayerJumpCallback.EVENT.register((player, world) -> {
            for (var i = -1; i <= 0; i++) {
                var pos = player.getBlockPos().add(0, i, 0);
                var state = world.getBlockState(pos);
                if (state.getBlock() instanceof PlayerJumpConsumingBlock block) {
                    block.onJump(state, world, pos, player);
                }
            }
        });
    }

    private int ward(CommandContext<ServerCommandSource> context, BlockState filter) throws CommandSyntaxException {
        var range = BlockBox.create(BlockPosArgumentType.getLoadedBlockPos(context, "from"), BlockPosArgumentType.getLoadedBlockPos(context, "to"));
        var source = context.getSource();
        var i = range.getBlockCountX() * range.getBlockCountY() * range.getBlockCountZ();
        if (i > 32768 * 8) {
            source.sendFeedback(() -> Text.translatable("commands.fill.toobig", 32768 * 8, i), false);
            return 0;
        }
        var serverWorld = source.getWorld();
        var j = 0;
        var ward = BoolArgumentType.getBool(context, "set");
        var chunks = new HashSet<Chunk>();
        for (var blockPos : BlockPos.iterate(range.getMinX(), range.getMinY(), range.getMinZ(), range.getMaxX(), range.getMaxY(), range.getMaxZ())) {
            var bs = serverWorld.getBlockState(blockPos);
            if (ward && bs.isAir()) {
                continue;
            }
            if (filter != null && bs != filter) continue;
            var chunk = serverWorld.getChunk(blockPos);
            var component = PeculiarComponentInitializer.WARDING.maybeGet(chunk);
            if (component.isPresent()) {
                var wardingComponent = component.get();
                wardingComponent.setWard(blockPos, ward);
                chunks.add(chunk);
                j++;
            }
        }
        for (var c : chunks) {
            PeculiarComponentInitializer.WARDING.sync(c);
        }
        final var fj = j;
        source.sendFeedback(() -> Text.translatable("peculiar.commands.wardarea.success", ward ? "Warded" : "Unwarded", fj), true);
        return j;
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}