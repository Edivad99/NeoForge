/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.oldtest;

import com.mojang.serialization.Codec;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
@Mod(DeferredRegistryTest.MODID)
public class DeferredRegistryTest {
    static final String MODID = "deferred_registry_test";
    private static final Logger LOGGER = LogManager.getLogger();

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final ResourceKey<Registry<Custom>> CUSTOM_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MODID, "test_registry"));
    private static final DeferredRegister<Custom> CUSTOMS = DeferredRegister.create(CUSTOM_REGISTRY_KEY, MODID);
    private static final DeferredRegister<Object> DOESNT_EXIST_REG = DeferredRegister.create(ResourceLocation.fromNamespaceAndPath(MODID, "doesnt_exist"), MODID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, MODID);
    // Vanilla Registry - filled directly after all RegistryEvent.Register events are fired
    private static final DeferredRegister<PosRuleTestType<?>> POS_RULE_TEST_TYPES = DeferredRegister.create(Registries.POS_RULE_TEST, MODID);

    private static final DeferredBlock<Block> BLOCK = BLOCKS.registerSimpleBlock("test", Block.Properties.of().mapColor(MapColor.STONE));
    private static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COMPONENT_TYPE = COMPONENTS.registerComponentType("test", builder -> builder.persistent(Codec.INT));
    private static final DeferredItem<BlockItem> ITEM = ITEMS.registerSimpleBlockItem(BLOCK);
    private static final DeferredItem<Item> ITEM_WITH_COMPONENT = ITEMS.registerItem("test_with_component", properties -> new Item(properties.component(COMPONENT_TYPE.get(), 3)));
    private static final DeferredHolder<Custom, Custom> CUSTOM = CUSTOMS.register("test", () -> new Custom() {});
    // Should never be created as the registry doesn't exist - this should silently fail and remain empty
    private static final DeferredHolder<Object, Object> DOESNT_EXIST = DOESNT_EXIST_REG.register("test", Object::new);
    private static final DeferredHolder<RecipeType<?>, RecipeType<?>> RECIPE_TYPE = RECIPE_TYPES.register("test", () -> new RecipeType<>() {});
    private static final DeferredHolder<PosRuleTestType<?>, PosRuleTestType<?>> POS_RULE_TEST_TYPE = POS_RULE_TEST_TYPES.register("test", () -> () -> null);

//    private static final TagKey<Custom> CUSTOM_TAG_KEY = CUSTOMS.createOptionalTagKey("test_tag", Set.of(CUSTOM));
    private static final Registry<Custom> CUSTOM_REG = CUSTOMS.makeRegistry(builder -> builder.onAdd((owner, id, key, obj) -> LOGGER.info("Custom Added: " + id + " " + obj.foo())));

    public DeferredRegistryTest(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        CUSTOMS.register(modBus);
        RECIPE_TYPES.register(modBus);
        POS_RULE_TEST_TYPES.register(modBus);
        modBus.addListener(this::gatherData);

        NeoForge.EVENT_BUS.addListener(this::serverStarted);
        modBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS)
            event.accept(ITEM);
    }

    public void serverStarted(ServerStartedEvent event) {
        // Validate all the RegistryObjects are filled / not filled
        BLOCK.get();
        ITEM.get();
        COMPONENT_TYPE.get();
        CUSTOM.get();
        if (DOESNT_EXIST.isBound())
            throw new IllegalStateException("DeferredRegistryTest#DOESNT_EXIST should not be present!");
        RECIPE_TYPE.get();
        //POS_RULE_TEST_TYPE.get();
        //PLACED_FEATURE.get();
    }

    public void gatherData(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();

        gen.addProvider(true, new ModelProvider(gen.getPackOutput(), MODID) {
            @Override
            protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
                blockModels.createTrivialBlock(BLOCK.value(), TexturedModel.CUBE.updateTexture(textures -> textures.put(TextureSlot.ALL, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top"))));
            }
        });
    }

    public static class Custom {
        public String foo() {
            return this.getClass().getName();
        }
    }
}
