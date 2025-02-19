/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.testframework.registration;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

public class DeferredItemBuilder<I extends Item> extends DeferredItem<I> {
    private final RegistrationHelper registrationHelper;

    protected DeferredItemBuilder(ResourceKey<Item> key, RegistrationHelper registrationHelper) {
        super(key);
        this.registrationHelper = registrationHelper;
    }

    public DeferredItemBuilder<I> withLang(String name) {
        registrationHelper.clientProvider(LanguageProvider.class, prov -> prov.add(value(), name));
        return this;
    }

    public DeferredItemBuilder<I> tab(ResourceKey<CreativeModeTab> tab) {
        registrationHelper.eventListeners().accept((final BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == tab) {
                event.accept(this);
            }
        });
        return this;
    }
}
