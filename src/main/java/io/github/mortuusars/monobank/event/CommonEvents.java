package io.github.mortuusars.monobank.event;

import io.github.mortuusars.monobank.Registry;
import io.github.mortuusars.monobank.config.Configuration;
import io.github.mortuusars.monobank.Thief;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import javax.annotation.Nullable;
import java.util.List;

public class CommonEvents {
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        Registry.Advancements.register();
    }

    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof LivingEntity opener)
                || opener.level.isClientSide
                || !Configuration.THIEF_INCLUDE_OTHER_CONTAINERS.get())
            return;

        BlockEntity blockEntity = opener.level.getBlockEntity(event.getHitVec().getBlockPos());
        if (blockEntity instanceof RandomizableContainerBlockEntity containerBE && containerBE.lootTable != null
                && Thief.isInProtectedStructureRange((ServerLevel)opener.getLevel(), opener.blockPosition())) {

            // Lootr compatibility:
            if (blockEntity.getBlockState().is(Registry.BlockTags.LOOTR_CONTAINERS)
                    && !Thief.shouldDeclareThiefForOpeningLootr(containerBE, opener)) {
                return;
            }

            List<LivingEntity> witnesses = Thief.getWitnesses(opener);
            if (witnesses.size() > 0)
                Thief.declareThief(opener, witnesses, Thief.Offence.MODERATE);
        }
    }

    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player.level instanceof ServerLevel serverLevel) || !Configuration.THIEF_INCLUDE_OTHER_CONTAINERS.get())
            return;

        BlockEntity blockEntityAtPos = player.level.getBlockEntity(event.getPos());
        if (blockEntityAtPos instanceof RandomizableContainerBlockEntity container && container.lootTable != null
                && Thief.isInProtectedStructureRange(serverLevel, player.blockPosition())) {

            List<LivingEntity> witnesses = Thief.getWitnesses(player);
            if (witnesses.size() > 0)
                Thief.declareThief(player, witnesses, Thief.Offence.HEAVY);
        }
    }

    public static void onEntityInteractEvent(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getPlayer();

        if (!(player.level instanceof ServerLevel serverLevel))
            return;

        if (Configuration.THIEF_NO_TRADE.get() && event.getTarget() instanceof Villager villager) {
            if (Configuration.THIEF_NO_TRADE_ONLY_NEAR_PROTECTED_STRUCTURES.get() &&
                    !Thief.isInProtectedStructureRange(serverLevel, player.blockPosition()))
                return;
            @Nullable MobEffectInstance thiefEffect = player.getEffect(Registry.Effects.THIEF.get());
            if (thiefEffect != null) {
                // Villagers will not trade with thieves.
                villager.setUnhappy();
                event.setCanceled(true);
            }
        }
    }
}