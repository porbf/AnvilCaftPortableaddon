package dev.anvilcraft.portableaddon.init;

import dev.anvilcraft.portableaddon.AnvilcraftPortableAddon;
import dev.anvilcraft.portableaddon.init.blocks.ender_transmission_pole.EnderPoleBlock;
import dev.anvilcraft.portableaddon.init.blocks.ender_transmission_pole.EnderPoleBlockEntity;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * 模组物品注册与管理类
 * 负责统一维护并向游戏事件总线注册所有自定义 Item、BlockItem 及其独特的交互行为
 */
public class AddonItems {

    // 延迟注册器 (DeferredRegister)，绑定当前模组的 MOD_ID
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnvilcraftPortableAddon.MOD_ID);

    // ==========================================
    // 1. 磁铁物品 (Magnet)
    // 核心行为：右键可以吸取世界上的铁砧，或者将内部存储的铁砧重新放回世界上
    // ==========================================
    public static DeferredItem<Item> Magnet =
            ITEMS.register("magnet", () -> new Item(new Item.Properties().stacksTo(1)) {

                @Override
                public InteractionResult useOn(UseOnContext context) {
                    // 仅在服务端且点击目标非空气方块时执行物理交互
                    if (!context.getLevel().isClientSide && !context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.AIR)) {
                        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
                        ItemStack _This = context.getItemInHand();
                        // 从物品的 DataComponent 中获取当前存储的铁砧状态 (BlockState)
                        BlockState AnvilState = context.getItemInHand().get(DataComponents.ANVIL_STATE);

                        if (AnvilState == null || AnvilState.equals(Blocks.AIR.defaultBlockState())) {
                            // 行为 A：磁铁当前为空，尝试吸取方块
                            if (blockState.is(BlockTags.ANVIL)) {
                                if (AnvilState == null || AnvilState.equals(Blocks.AIR.defaultBlockState())) {
                                    // 将目标铁砧方块状态写入磁铁的 DataComponent，并将世界上的铁砧方块抹除（变成空气）
                                    _This.set(DataComponents.ANVIL_STATE, blockState);
                                    context.getLevel().setBlock(context.getClickedPos(), Blocks.AIR.defaultBlockState(), 3);
                                    return InteractionResult.SUCCESS; // 拦截并结束交互事件
                                }
                            }
                        }
                        else {
                            // 行为 B：磁铁已充能（含有铁砧），尝试在点击面外侧放置铁砧
                            BlockPos _TargetBlock = context.getClickedPos().relative(context.getClickedFace());
                            if (context.getLevel().getBlockState(_TargetBlock).is(Blocks.AIR)) {
                                // 提取磁铁里的铁砧状态并生成到世界上，随后重置磁铁数据为空气
                                context.getLevel().setBlock(_TargetBlock, Objects.requireNonNull(_This.get(DataComponents.ANVIL_STATE.get())), 3);
                                _This.set(DataComponents.ANVIL_STATE, Blocks.AIR.defaultBlockState());
                                return InteractionResult.SUCCESS; // 拦截并结束交互事件
                            } else {
                                return InteractionResult.FAIL; // 目标位置被堵住，放置失败
                            }
                        }
                        return InteractionResult.SUCCESS;
                    }
                    return super.useOn(context);
                }

                @Override
                public boolean isFoil(ItemStack stack) {
                    // 动态附魔光效应：当磁铁内部成功携带了有效的铁砧时，物品图标在物品栏会自带附魔流光效果
                    return stack.get(DataComponents.ANVIL_STATE) != null && !stack.get(DataComponents.ANVIL_STATE).is(Blocks.AIR);
                }

                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    // 动态本地化信息提示：根据磁铁内部是否装载铁砧，显示不同的 Hover 提示与具体的铁砧物品名称
                    if (stack.get(DataComponents.ANVIL_STATE) != null && !stack.get(DataComponents.ANVIL_STATE).is(Blocks.AIR)) {
                        tooltipComponents.add(Component.translatable("item.anvilcraftportableaddon.magnet.AVONtooltip").append(
                                Component.literal(stack.get(DataComponents.ANVIL_STATE).getBlock().asItem().toString())
                        ));
                    } else {
                        tooltipComponents.add(Component.translatable("item.anvilcraftportableaddon.magnet.AVOFFtooltip"));
                    }
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            });

    // ==========================================
    // 2. 末影电线杆物品 (ENDERPOLE_ITEM)
    // 核心行为：既是电线杆的放置物，同时也是多方块结构的跨维度无线连线配置工具
    // ==========================================
    public static final DeferredItem<BlockItem> ENDERPOLE_ITEM =
            ITEMS.register("enderpole", () -> new BlockItem(AddonBlocks.ENDERPOLE.get(), new Item.Properties()) {

                @Override
                public InteractionResult useOn(UseOnContext context) {
                    // 连接配置行为：右键点击一个有效的电线杆方块实体
                    if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof EnderPoleBlockEntity B_E) {
                        ItemStack _This = context.getItemInHand();

                        // 重定向逻辑：不论玩家点击了三格高电线杆的哪一格，自动寻找并定位到关键的“顶部段(TOP)”方块实体
                        BlockEntity TState = switch (B_E.getBlockState().getValue(EnderPoleBlock.PARTHALF)){
                            case Vertical3PartHalf.TOP -> context.getLevel().getBlockEntity(context.getClickedPos());
                            case Vertical3PartHalf.MID -> context.getLevel().getBlockEntity(context.getClickedPos().above(1));
                            case Vertical3PartHalf.BOTTOM -> context.getLevel().getBlockEntity(context.getClickedPos().above(2));
                        };

                        // 将顶部段的物理坐标 (BlockPos) 与其当前所在的世界维度 (Dimension Key) 记录入手中工具的 DataComponent 中
                        _This.set(DataComponents.TARGET_POS, TState.getBlockPos());
                        _This.set(DataComponents.TARGET_DIM, TState.getLevel().dimension().location());

                        // 向客户端玩家发送一条操作成功的动作栏动作提示消息
                        context.getPlayer().displayClientMessage(Component.translatable("item.anvilcraftportableaddon.enderpole.tooltip"), true);
                        return InteractionResult.SUCCESS;
                    }
                    return super.useOn(context);
                }

                @Override
                protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
                    // 1. 刚放下去时，只有当前位置（BOTTOM）的实体是绝对存在的
                    if (level.getBlockEntity(pos) instanceof EnderPoleBlockEntity be) {
                        ResourceLocation dim = stack.get(DataComponents.TARGET_DIM);
                        BlockPos targetPos = stack.get(DataComponents.TARGET_POS);

                        if (dim != null && targetPos != null) {
                            // 先把连接数据稳妥地存在 BOTTOM 身上！
                            be.TPos = targetPos;
                            // 存入维度 ID，不在这里急着拿 Level 对象，防止空指针
                            be.TDim = dim;
                            be.setChanged();
                        }
                    }
                    return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
                }
            });

    // ==========================================
    // 3. 基础能源方块物品 (POWER_BLOCK_ITEM)
    // 快捷将常规方块转换为对应 BlockItem 的简易注册
    // ==========================================
    public static final DeferredItem<BlockItem> POWER_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("power_block", AddonBlocks.POWER_BLOCK);


    /**
     * 初始化物品注册
     * @param eventBus 模组专用的 FML/NeoForge 事件总线
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}