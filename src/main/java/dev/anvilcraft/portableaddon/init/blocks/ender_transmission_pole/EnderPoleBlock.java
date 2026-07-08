package dev.anvilcraft.portableaddon.block.blocks.ender_transmission_pole;

import dev.anvilcraft.portableaddon.block.ModBlocks;
import com.sun.jna.platform.unix.solaris.LibKstat;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 末影传输电线杆方块
 * 继承自 SimpleMultiPartBlock，表现为 3 格高的垂直多部分方块结构
 */
public class EnderPoleBlock extends SimpleMultiPartBlock<Vertical3PartHalf> implements IHasMultiBlock, EntityBlock {

    public EnderPoleBlock() {
        super(Properties.of()
                .strength(2.0F)
                // 动态发光：当电线杆处于“过载”且开关为“开启”状态时发出 15 级满亮度光，否则不发光
                .lightLevel(state -> state.getValue(OVERLOAD) && state.getValue(SWITCH) == IPowerComponent.Switch.ON ? 15 : 0)
        );
        // 注册方块的默认状态属性
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PARTHALF, Vertical3PartHalf.BOTTOM) // 默认表现为底部段
                .setValue(OVERLOAD, true)                     // 默认开启过载检测
                .setValue(SWITCH, IPowerComponent.Switch.OFF) // 默认开关关闭
                .setValue(IsSon, false)                     // 默认为主端
        );
    }

    // --- 方块状态属性定义 (BlockState Properties) ---
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final BooleanProperty IsSon = BooleanProperty.create("is_son");             // 该格是否为从属核心
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH; // 电源开关状态 (ON/OFF)
    public static final EnumProperty<Vertical3PartHalf> PARTHALF = EnumProperty.create("half", Vertical3PartHalf.class); // 区分垂直方向的哪一段 (BOTTOM/MID/TOP)

    // --- 碰撞箱 (VoxelShape) 定义 ---
    public static final VoxelShape SHAPE_TOP = Shapes.or(Block.box(3, 5, 3, 13, 16, 13), Block.box(6, 0, 6, 10, 5, 10));
    public static final VoxelShape SHAPE_MID = Block.box(6, 0, 6, 10, 16, 10);
    public static final VoxelShape SHAPE_BOT = Shapes.or(Block.box(3, 4, 3, 13, 10, 13), Block.box(0, 0, 0, 16, 4, 16), Block.box(6, 10, 6, 10, 16, 10));

    @Override
    public void onRemove(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        // TODO: 方块破坏或被移除时的清理逻辑（如断开连线、掉落物品等）
    }

    @Override
    public void onPlace(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        // TODO: 方块放置时的初始化逻辑
    }

    @Override
    public @NotNull Property<Vertical3PartHalf> getPart() {
        return PARTHALF; // 返回控制方块分段的属性映射
    }

    @Override
    public Vertical3PartHalf @NotNull [] getParts() {
        return Vertical3PartHalf.values(); // 返回全部分段的枚举数组
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // 根据当前所处的具体段（上、中、下），返回对应的独立碰撞体积箱
        return switch (state.getValue(PARTHALF)) {
            case BOTTOM -> SHAPE_BOT;
            case MID -> SHAPE_MID;
            case TOP -> SHAPE_TOP;
        };
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL; // 使用常规的 json/obj 模型进行渲染
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new EnderPoleBlockEntity(blockPos, blockState); // 绑定专用的方块实体
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 向方块状态构建器注册所有自定义的属性，使其能存入 BlockState
        builder.add(PARTHALF).add(OVERLOAD).add(SWITCH).add(IsSon);
    }

    @Override
    public @Nullable BlockState getPlacementState(@NotNull BlockPlaceContext context) {
        // 玩家手动放置时的初始状态：表现为底部段，非主控
        return defaultBlockState().setValue(PARTHALF, Vertical3PartHalf.BOTTOM).setValue(IsSon, false);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null; // 过滤客户端，只在服务端注册 tick 逻辑
        return (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof EnderPoleBlockEntity poleEntity) {
                poleEntity.tick(level1, pos, state1); // 每 tick 执行方块实体的具体业务
            }
        };
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block neighborBlock, @NotNull BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        // 限制：仅由电线杆的“底部段”来响应邻近方块的红石信号更新
        if (state.getValue(PARTHALF) != Vertical3PartHalf.BOTTOM) return;

        // 向上获取两格处的“顶部段”方块状态
        BlockPos topPos = pos.above(2);
        BlockState topState = level.getBlockState(topPos);
        if (!topState.is(ModBlocks.ENDERPOLE.get()) || topState.getValue(PARTHALF) != Vertical3PartHalf.TOP) return;

        // 检查底部是否接收到红石信号
        boolean hasRedstoneSignal = level.hasNeighborSignal(pos);
        boolean isCurrentlyOff = (state.getValue(SWITCH) == IPowerComponent.Switch.OFF);

        // 红石联动逻辑：有信号时强制设为 OFF（切断能源），无信号时恢复 ON（开启能源）
        if (isCurrentlyOff != hasRedstoneSignal) {
            IPowerComponent.Switch nextSwitch = hasRedstoneSignal ? IPowerComponent.Switch.OFF : IPowerComponent.Switch.ON;

            BlockState updatedBottom = state.setValue(SWITCH, nextSwitch);
            BlockState updatedTop = topState.setValue(SWITCH, nextSwitch);

            // 同时更新底部和顶部的方块状态
            level.setBlockAndUpdate(pos, updatedBottom);
            level.setBlockAndUpdate(topPos, updatedTop);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 1. 核心安全：只在服务端处理逻辑和发送消息，客户端直接返回 SUCCESS 结束
        if (level.isClientSide) return InteractionResult.SUCCESS;

        // 精准定位到顶部实体来读取数据
        EnderPoleBlockEntity topEntity = switch (state.getValue(PARTHALF)) {
            case TOP -> (EnderPoleBlockEntity) level.getBlockEntity(pos);
            case MID -> (EnderPoleBlockEntity) level.getBlockEntity(pos.above(1));
            case BOTTOM -> (EnderPoleBlockEntity) level.getBlockEntity(pos.above(2));
        };

        if (topEntity == null) return InteractionResult.SUCCESS;

        // 2. 换回传统的字符串拼接（+ 号连接），安全且无版本依赖问题
        String msg;
        if (state.getValue(IsSon)) {
            String posStr = (topEntity.TPos != null) ? topEntity.TPos.toShortString() : "未连接/无数据";
            String dimStr = (topEntity.OuterPoleLevel != null) ? topEntity.OuterPoleLevel.dimension().location().toString() : "未知维度";

            msg = "当前状态：[子杆] | 连接坐标: " + posStr + " | 维度: " + dimStr + " | 连接状态: " + topEntity.netpowerisrelly;
        } else {
            msg = "当前状态：[父杆] | 连接有效性: " + topEntity.netpowerisrelly;
        }

        // 发送消息
        player.displayClientMessage(Component.literal(msg), false);
        return InteractionResult.SUCCESS;
    }
}