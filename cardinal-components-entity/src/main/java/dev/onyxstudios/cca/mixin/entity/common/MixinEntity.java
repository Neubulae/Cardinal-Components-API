/*
 * Cardinal-Components-API
 * Copyright (C) 2019-2021 OnyxStudios
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dev.onyxstudios.cca.mixin.entity.common;

import com.google.common.collect.Iterators;
import dev.onyxstudios.cca.api.v3.component.ComponentContainer;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.sync.ComponentPacketWriter;
import dev.onyxstudios.cca.internal.base.InternalComponentProvider;
import dev.onyxstudios.cca.internal.entity.CardinalEntityInternals;
import nerdhub.cardinal.components.CardinalComponentsEntity;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;

@Mixin(Entity.class)
public abstract class MixinEntity implements InternalComponentProvider {
    @Unique
    private ComponentContainer components;

    @Shadow
    public abstract EntityType<?> getType();

    @Shadow
    public World world;

    @Shadow
    public abstract int getEntityId();

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void initDataTracker(CallbackInfo ci) {
        this.components = CardinalEntityInternals.createEntityComponentContainer((Entity) (Object) this);
    }

    @Inject(method = "toTag", at = @At("RETURN"))
    private void toTag(CompoundTag inputTag, CallbackInfoReturnable<CompoundTag> cir) {
        this.components.toTag(cir.getReturnValue());
    }

    @Inject(method = "fromTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;readCustomDataFromTag(Lnet/minecraft/nbt/CompoundTag;)V", shift = At.Shift.AFTER))
    private void fromTag(CompoundTag tag, CallbackInfo ci) {
        this.components.fromTag(tag);
    }

    @Nonnull
    @Override
    public ComponentContainer getComponentContainer() {
        return this.components;
    }

    @Override
    public Iterator<ServerPlayerEntity> getRecipientsForComponentSync() {
        Entity holder = (Entity) (Object) this;
        if (!this.world.isClient) {
            Iterator<ServerPlayerEntity> watchers = PlayerStream.watching(holder).map(ServerPlayerEntity.class::cast).iterator();
            //noinspection ConstantConditions
            if (holder instanceof ServerPlayerEntity && ((ServerPlayerEntity) holder).networkHandler != null) {
                return Iterators.concat(Iterators.singletonIterator((ServerPlayerEntity)holder), watchers);
            }
            return watchers;
        }
        return Collections.emptyIterator();
    }

    @Nullable
    @Override
    public <C extends AutoSyncedComponent> CustomPayloadS2CPacket toComponentPacket(PacketByteBuf buf, ComponentKey<? super C> key, ComponentPacketWriter writer, ServerPlayerEntity recipient) {
        buf.writeInt(this.getEntityId());
        buf.writeIdentifier(key.getId());
        writer.writeSyncPacket(buf, recipient);
        return new CustomPayloadS2CPacket(CardinalComponentsEntity.PACKET_ID, buf);
    }

    @Override
    public boolean supportsCustomComponentPacketWriters() {
        return true;
    }
}
