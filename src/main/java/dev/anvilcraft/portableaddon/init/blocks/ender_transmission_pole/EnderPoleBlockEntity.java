package dev.anvilcraft.portableaddon.block.blocks.ender_transmission_pole;

import dev.anvilcraft.portableaddon.block.ModBlockEntity;
import dev.dubhe.anvilcraft.api.power.IPowerTransmitter;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 末影传输电线杆的方块实体 (BlockEntity)
 * 实现 IPowerTransmitter 用于在 AnvilCraft 电网中传输电力
 */
public class EnderPoleBlockEntity extends BlockEntity implements IPowerTransmitter {

    // --- 电网与连接属性 ---
    public boolean netpowerisrelly = true;  // 表示电网连接是否有效
    public @Nullable PowerGrid IGrid = null;           //自身所处电网
    public @Nullable EnderPoleBlockEntity OuterPole = null;        //另一个杆子
    public @Nullable ServerLevel OuterPoleLevel = null;
    public @Nullable EnderPoleBlockEntity PartTop = null;          //获取顶部
    public @Nullable EnderPoleBlockEntity PartBottom = null;          //获取底部

    public @Nullable BlockPos TPos = null;
    public @Nullable ResourceLocation TDim = null;

    // --- 构造函数与工厂方法 ---
    public EnderPoleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntity.ENDER_POLE_ENTITY.get(), pos, blockState);
    }

    // --- 基础接口实现 ---
    @Override
    public @Nullable Level getCurrentLevel() { return this.getLevel(); }

    @Override
    public @NotNull BlockPos getPos() { return this.getBlockPos(); }

    @Override
    public void setGrid(@Nullable PowerGrid powerGrid) { this.IGrid = powerGrid; }

    @Override
    public @Nullable PowerGrid getGrid() { return this.IGrid; }

    /**
     * 每 tick 执行的核心逻辑（仅在服务端运行）
     * 负责维护与远程电线杆的跨维度连接、验证结构合法性，并实时动态注册/注销电网
     */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        state = level.getBlockState(pos);


        // ================ 1. 初始化阶段：顶部从底部白嫖数据 ================
        if (state.getValue(EnderPoleBlock.PARTHALF) == Vertical3PartHalf.TOP) {
            this.PartTop = this;
            if (this.level != null) {
                BlockEntity botBe = this.level.getBlockEntity(pos.below(2));
                if (botBe instanceof EnderPoleBlockEntity bottomPole) {
                    this.PartBottom = bottomPole;
                    // 如果底部存了未处理的物品连接数据，拿上来！
                    if (bottomPole.TDim != null && bottomPole.TPos != null) {
                        this.TPos = bottomPole.TPos;
                        this.OuterPoleLevel = Objects.requireNonNull(level.getServer())
                            .getLevel(ResourceKey.create(Registries.DIMENSION, bottomPole.TDim));

                        // 真正安全地把当前方块状态改为子杆！
                        level.setBlockAndUpdate(pos, state.setValue(EnderPoleBlock.IsSon, true));
                        state = level.getBlockState(pos); // 刷新局部变量 state

                        // 清除底部的暂存，防止重复触发
                        bottomPole.TDim = null;
                        this.setChanged();
                    }
                }
            }
        }

        if (state.getValue(EnderPoleBlock.SWITCH) == Switch.ON) {
            if (state.getValue(EnderPoleBlock.PARTHALF) == Vertical3PartHalf.TOP) {
                if (state.getValue(EnderPoleBlock.IsSon)) { // 已经是子杆了
                    // 重新获取远程父杆实体
                    if (this.TPos != null && this.OuterPoleLevel != null) {
                        BlockEntity remoteBe = this.OuterPoleLevel.getBlockEntity(this.TPos);
                        if (remoteBe instanceof EnderPoleBlockEntity remotePole) {
                            this.OuterPole = remotePole;
                        } else {
                            this.OuterPole = null;
                        }
                    }

                    if (this.OuterPole == null) {
                        // 找不到父杆，变成父杆
                        level.setBlockAndUpdate(pos, state.setValue(EnderPoleBlock.IsSon, false));
                        return;
                    }

                    // 电网握手
                    if (this.getGrid() == null || this.OuterPole.IGrid != this.IGrid) {
                        if (this.OuterPole.IGrid != null && OuterPole.getBlockState().getValue(EnderPoleBlock.SWITCH) == Switch.ON) {
                            this.OuterPole.IGrid.add(this);
                            this.IGrid = this.OuterPole.IGrid;
                        }
                    }
                }
            }
        } else if (state.getValue(EnderPoleBlock.PARTHALF) == Vertical3PartHalf.BOTTOM) {
            // 底部负责关机断电
            BlockEntity topBe = level.getBlockEntity(pos.above(2));
            if (topBe instanceof EnderPoleBlockEntity topPole && topPole.getGrid() != null) {
                PowerGrid.removeComponent(topPole);
                topPole.IGrid = null;
            }
        }

        // ================ 3. 连接有效性验证 ================
        if (state.getValue(EnderPoleBlock.PARTHALF) == Vertical3PartHalf.TOP) {
            if (state.getValue(EnderPoleBlock.SWITCH) == Switch.ON) {
                if (state.getValue(EnderPoleBlock.IsSon)) {
                    this.netpowerisrelly = this.OuterPole != null && this.getGrid() == this.OuterPole.IGrid;
                } else {
                    this.netpowerisrelly = true;
                }
            } else {
                this.netpowerisrelly = false;
            }
        }
        // 真正安全地把当前方块状态改为子杆！
        level.setBlockAndUpdate(pos, state.setValue(EnderPoleBlock.IsSon, true));

        // 【重要】这一步必须加上！重新抓取世界上最新的 BlockState，否则你下面 tick 里剩下的逻辑用的还是旧的 state (IsSon=false)
        this.setChanged();
        this.flushState(level, pos);
    }

    /**
     * 定义该设备在电网中的角色
     * 逻辑核心：通过 Net power 的正负号和“父子节点(IsFather)”身份，动态反转生产者/消费者角色
     */
    @Override
    public @NotNull PowerComponentType getComponentType() {
        // 功率为0，或当前不是“顶部段”，则在电网中视为无效组件
        if (this.level != null) {
            if (!netpowerisrelly || this.getBlockState().getValue(EnderPoleBlock.PARTHALF) != Vertical3PartHalf.TOP) {
                if (this.getBlockState().getValue(EnderPoleBlock.PARTHALF) == Vertical3PartHalf.BOTTOM) {
                    this.PartTop = (EnderPoleBlockEntity) this.level.getBlockEntity(this.getBlockPos().above(2));
                    this.PartBottom = this;
                    return PowerComponentType.INVALID;
                } else {
                    this.PartTop = (EnderPoleBlockEntity) this.level.getBlockEntity(this.getBlockPos().above(1));
                    this.PartBottom = (EnderPoleBlockEntity) this.level.getBlockEntity(this.getBlockPos().below(1));
                    return PowerComponentType.INVALID;
                }
            }
        } else return PowerComponentType.INVALID;
        this.PartTop = this;
        if (this.level != null) {
            this.PartBottom = (EnderPoleBlockEntity) this.level.getBlockEntity(this.getBlockPos().below(2));
        }
        return PowerComponentType.TRANSMITTER;
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.OuterPoleLevel != null) tag.putString("OuterPoleLevelDim", this.OuterPoleLevel.dimension().location().toString());
        else if (this.TDim != null) tag.putString("OuterPoleLevelDim", this.TDim.toString());
        if (this.TPos != null) tag.putLong("TPos", this.TPos.asLong());
        tag.putBoolean("netpowerisrelly", this.netpowerisrelly);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("OuterPoleLevelDim")) {
            this.TDim = ResourceLocation.parse(tag.getString("OuterPoleLevelDim"));
        }
        if (tag.contains("TPos")) {
            this.TPos = BlockPos.of(tag.getLong("TPos"));
        }
        this.netpowerisrelly = tag.getBoolean("netpowerisrelly");
    }
    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide && this.TDim != null) {
            this.OuterPoleLevel = Objects.requireNonNull(this.level.getServer()).getLevel(ResourceKey.create(Registries.DIMENSION, this.TDim));
        }
    }
}