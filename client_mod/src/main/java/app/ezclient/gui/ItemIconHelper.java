package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemIconHelper {
    private static final Map<String, ItemStack> BLOCK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ItemStack> ENTITY_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean componentsBound = false;

    private ItemIconHelper() {}

    public static synchronized void ensureComponentsBound() {
        if (componentsBound) return;
        try {
            var provider = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(provider).forEach(DataComponentInitializers.PendingComponents::apply);
            app.ezclient.EzClientMod.log("Data components initialized via BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.");
        } catch (Throwable t) {
            app.ezclient.EzClientMod.log("Note: DATA_COMPONENT_INITIALIZERS.build fallback: " + t.getMessage());
        }

        try {
            for (Item item : BuiltInRegistries.ITEM) {
                ensureItemModel(item);
            }
        } catch (Throwable ignored) {}

        componentsBound = true;
    }

    public static void ensureItemModel(Item item) {
        if (item == null || item == Items.AIR) return;
        try {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) return;
            var holder = item.builtInRegistryHolder();
            var existing = holder.areComponentsBound() ? holder.components() : DataComponentMap.EMPTY;
            if (!existing.has(DataComponents.ITEM_MODEL)) {
                var builder = DataComponentMap.builder();
                builder.addAll(existing);
                builder.set(DataComponents.ITEM_MODEL, id);
                if (!existing.has(DataComponents.ITEM_NAME)) {
                    builder.set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.translatable(item.getDescriptionId()));
                }
                if (!existing.has(DataComponents.MAX_STACK_SIZE)) {
                    builder.set(DataComponents.MAX_STACK_SIZE, item.getDefaultMaxStackSize());
                }
                holder.bindComponents(builder.build());
            }
        } catch (Throwable ignored) {}
    }

    public static ItemStack createSafeStack(Item item) {
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ensureComponentsBound();
        ensureItemModel(item);
        try {
            return new ItemStack(item);
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack getBlockIcon(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return createSafeStack(Items.STONE);
        String trimmed = rawKey.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("entity:")) return getEntityIcon(trimmed);
        String cleanKey = trimmed.startsWith("block:") ? trimmed.substring(6) : trimmed;

        ItemStack cached = BLOCK_CACHE.get(cleanKey);
        if (cached != null && !cached.isEmpty()) return cached;

        ItemStack resolved = resolveBlockIcon(cleanKey);
        if (resolved != null && !resolved.isEmpty()) {
            BLOCK_CACHE.put(cleanKey, resolved);
            return resolved;
        }
        return createSafeStack(Items.STONE);
    }

    private static ItemStack resolveBlockIcon(String key) {
        try {
            Identifier id = Identifier.tryParse(key);
            if (id != null) {
                // 1. Direct block.asItem()
                Block block = BuiltInRegistries.BLOCK.getValue(id);
                if (block != null) {
                    Item item = block.asItem();
                    if (item != null && item != Items.AIR) {
                        ItemStack s = createSafeStack(item);
                        if (!s.isEmpty()) return s;
                    }
                }
                // 2. Direct item registry lookup with the same identifier
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (item != null && item != Items.AIR) {
                    ItemStack s = createSafeStack(item);
                    if (!s.isEmpty()) return s;
                }
                // 3. Fallback to alias resolver for blocks without a direct BlockItem
                Item aliasItem = getBlockAliasItem(id.getNamespace(), id.getPath());
                if (aliasItem != null && aliasItem != Items.AIR) {
                    ItemStack s = createSafeStack(aliasItem);
                    if (!s.isEmpty()) return s;
                }
            }
        } catch (Throwable ignored) {}
        return ItemStack.EMPTY;
    }

    public static ItemStack getEntityIcon(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return createSafeStack(Items.NAME_TAG);
        String trimmed = rawKey.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("block:")) return getBlockIcon(trimmed);
        String cleanKey = trimmed.startsWith("entity:") ? trimmed.substring(7) : trimmed;

        ItemStack cached = ENTITY_CACHE.get(cleanKey);
        if (cached != null && !cached.isEmpty()) return cached;

        ItemStack resolved = resolveEntityIcon(cleanKey);
        if (resolved != null && !resolved.isEmpty()) {
            ENTITY_CACHE.put(cleanKey, resolved);
            return resolved;
        }
        return createSafeStack(Items.NAME_TAG);
    }

    private static ItemStack resolveEntityIcon(String key) {
        try {
            Identifier id = Identifier.tryParse(key);
            if (id != null) {
                String namespace = id.getNamespace();
                String path = id.getPath();

                // 1. Try spawn egg convention (<path>_spawn_egg)
                Identifier eggId = Identifier.fromNamespaceAndPath(namespace, path + "_spawn_egg");
                Item eggItem = BuiltInRegistries.ITEM.getValue(eggId);
                if (eggItem != null && eggItem != Items.AIR) {
                    ItemStack s = createSafeStack(eggItem);
                    if (!s.isEmpty()) return s;
                }

                // 2. Try SpawnEggItem.byId
                var entityType = BuiltInRegistries.ENTITY_TYPE.getValue(id);
                if (entityType != null) {
                    var egg = SpawnEggItem.byId(entityType);
                    if (egg != null && egg.isPresent()) {
                        ItemStack s = createSafeStack(egg.get().value());
                        if (!s.isEmpty()) return s;
                    }
                }

                // 3. Try direct item matching entity name (boats, minecarts, projectiles, frames, etc.)
                Item directItem = BuiltInRegistries.ITEM.getValue(id);
                if (directItem != null && directItem != Items.AIR) {
                    ItemStack s = createSafeStack(directItem);
                    if (!s.isEmpty()) return s;
                }

                // 4. Special entity item mapping
                Item special = getEntitySpecialItem(path);
                if (special != null && special != Items.AIR) {
                    ItemStack s = createSafeStack(special);
                    if (!s.isEmpty()) return s;
                }
            }
        } catch (Throwable ignored) {}
        return ItemStack.EMPTY;
    }

    public static void renderEntryIcon(GuiGraphicsExtractor g, String key, boolean isEntity, int x, int y) {
        boolean actualIsEntity = isEntity;
        if (key != null) {
            String lower = key.trim().toLowerCase(Locale.ROOT);
            if (lower.startsWith("entity:")) actualIsEntity = true;
            else if (lower.startsWith("block:")) actualIsEntity = false;
        }

        EzUi.roundedRect(g, x, y, 16, 16, 3, 0x3015181C);
        EzUi.roundedRect(g, x, y, 16, 16, 3, actualIsEntity ? 0x3522C55E : 0x35F59E0B);

        try {
            ItemStack stack = actualIsEntity ? getEntityIcon(key) : getBlockIcon(key);
            if (stack != null && !stack.isEmpty()) {
                g.item(stack, x, y);
            }
        } catch (Throwable ignored) {}
    }

    private static Item getBlockAliasItem(String namespace, String path) {
        // Fluids & cauldrons
        if ("water".equals(path) || "flowing_water".equals(path) || "water_cauldron".equals(path) || "bubble_column".equals(path)) {
            return Items.WATER_BUCKET;
        }
        if ("lava".equals(path) || "flowing_lava".equals(path) || "lava_cauldron".equals(path)) {
            return Items.LAVA_BUCKET;
        }
        if ("powder_snow_cauldron".equals(path) || "powder_snow".equals(path)) {
            return Items.POWDER_SNOW_BUCKET;
        }

        // Fire & portals
        if ("fire".equals(path)) return Items.FLINT_AND_STEEL;
        if ("soul_fire".equals(path)) return Items.SOUL_CAMPFIRE;
        if ("nether_portal".equals(path)) return Items.OBSIDIAN;
        if ("end_portal".equals(path)) return Items.END_PORTAL_FRAME;
        if ("end_gateway".equals(path)) return Items.END_STONE;

        // Redstone & wires
        if ("redstone_wire".equals(path)) return Items.REDSTONE;
        if ("tripwire".equals(path)) return Items.STRING;

        // Crops & plants without a 1:1 named block item
        if ("potatoes".equals(path)) return Items.POTATO;
        if ("carrots".equals(path)) return Items.CARROT;
        if ("wheat".equals(path)) return Items.WHEAT;
        if ("beetroots".equals(path)) return Items.BEETROOT;
        if ("sweet_berry_bush".equals(path)) return Items.SWEET_BERRIES;
        if ("cocoa".equals(path)) return Items.COCOA_BEANS;
        if ("melon_stem".equals(path) || "attached_melon_stem".equals(path)) return Items.MELON_SEEDS;
        if ("pumpkin_stem".equals(path) || "attached_pumpkin_stem".equals(path)) return Items.PUMPKIN_SEEDS;
        if ("torchflower_crop".equals(path)) return Items.TORCHFLOWER_SEEDS;
        if ("pitcher_crop".equals(path)) return Items.PITCHER_POD;
        if ("bamboo_sapling".equals(path)) return Items.BAMBOO;
        if ("sugar_cane".equals(path)) return Items.SUGAR_CANE;
        if ("kelp_plant".equals(path)) return Items.KELP;
        if ("weeping_vines_plant".equals(path)) return Items.WEEPING_VINES;
        if ("twisting_vines_plant".equals(path)) return Items.TWISTING_VINES;
        if ("cave_vines".equals(path) || "cave_vines_plant".equals(path)) return Items.GLOW_BERRIES;
        if ("big_dripleaf_stem".equals(path)) return Items.BIG_DRIPLEAF;

        // Pistons & ice & air
        if ("moving_piston".equals(path) || "piston_head".equals(path)) return Items.PISTON;
        if ("frosted_ice".equals(path)) return Items.ICE;
        if ("air".equals(path) || "cave_air".equals(path) || "void_air".equals(path)) return Items.BARRIER;

        // Candle cakes
        if (path.endsWith("_candle_cake") || "candle_cake".equals(path)) return Items.CAKE;

        // Wall hanging signs: e.g. spruce_wall_hanging_sign -> spruce_hanging_sign
        if (path.endsWith("_wall_hanging_sign")) {
            String base = path.substring(0, path.length() - 18) + "_hanging_sign";
            Item it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace, base));
            if (it != null && it != Items.AIR) return it;
        }

        // Wall signs: e.g. spruce_wall_sign -> spruce_sign
        if (path.endsWith("_wall_sign")) {
            String base = path.substring(0, path.length() - 10) + "_sign";
            Item it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace, base));
            if (it != null && it != Items.AIR) return it;
        }

        // Wall banners: e.g. red_wall_banner -> red_banner
        if (path.endsWith("_wall_banner")) {
            String base = path.substring(0, path.length() - 12) + "_banner";
            Item it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace, base));
            if (it != null && it != Items.AIR) return it;
        }

        // Wall heads: e.g. zombie_wall_head -> zombie_head
        if (path.endsWith("_wall_head")) {
            String base = path.substring(0, path.length() - 10) + "_head";
            Item it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace, base));
            if (it != null && it != Items.AIR) return it;
        }

        // Wall skulls: e.g. skeleton_wall_skull -> skeleton_skull
        if (path.endsWith("_wall_skull")) {
            String base = path.substring(0, path.length() - 11) + "_skull";
            Item it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace, base));
            if (it != null && it != Items.AIR) return it;
        }

        // Wall torches: e.g. soul_wall_torch -> soul_torch, wall_torch -> torch
        if ("wall_torch".equals(path)) return Items.TORCH;
        if (path.endsWith("_wall_torch")) {
            String base = path.substring(0, path.length() - 11) + "_torch";
            Item it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace, base));
            if (it != null && it != Items.AIR) return it;
        }

        // Coral wall fans: e.g. tube_coral_wall_fan -> tube_coral_fan
        if (path.endsWith("_wall_fan")) {
            String base = path.substring(0, path.length() - 9) + "_fan";
            Item it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace, base));
            if (it != null && it != Items.AIR) return it;
        }

        // Potted plants: e.g. potted_poppy -> poppy
        if (path.startsWith("potted_")) {
            String plant = path.substring(7);
            Item it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace, plant));
            if (it != null && it != Items.AIR) return it;
            if ("azalea_bush".equals(plant)) return Items.AZALEA;
            if ("flowering_azalea_bush".equals(plant)) return Items.FLOWERING_AZALEA;
            if ("fern".equals(plant)) return Items.FERN;
            return Items.FLOWER_POT;
        }

        return null;
    }

    private static Item getEntitySpecialItem(String path) {
        return switch (path) {
            case "player" -> Items.PLAYER_HEAD;
            case "ender_dragon" -> Items.DRAGON_HEAD;
            case "wither" -> Items.NETHER_STAR;
            case "iron_golem" -> Items.IRON_BLOCK;
            case "snow_golem" -> Items.CARVED_PUMPKIN;
            case "giant" -> Items.ZOMBIE_HEAD;
            case "warden" -> Items.ECHO_SHARD;
            case "illusioner" -> Items.BOW;
            case "experience_orb", "experience_bottle" -> Items.EXPERIENCE_BOTTLE;
            case "splash_potion" -> Items.SPLASH_POTION;
            case "lingering_potion" -> Items.LINGERING_POTION;
            case "potion" -> Items.POTION;
            case "fireball", "small_fireball", "dragon_fireball" -> Items.FIRE_CHARGE;
            case "wind_charge", "breeze_wind_charge" -> Items.WIND_CHARGE;
            case "wither_skull" -> Items.WITHER_SKELETON_SKULL;
            case "shulker_bullet" -> Items.SHULKER_SHELL;
            case "spawner_minecart" -> Items.MINECART;
            case "fishing_bobber" -> Items.FISHING_ROD;
            case "lightning_bolt" -> itemByName("lightning_rod");
            case "falling_block" -> Items.SAND;
            case "area_effect_cloud" -> Items.DRAGON_BREATH;
            case "evoker_fangs" -> Items.TOTEM_OF_UNDYING;
            case "ominous_item_spawner" -> Items.TRIAL_KEY;
            case "marker", "interaction" -> Items.BARRIER;
            case "item_display" -> Items.ITEM_FRAME;
            case "block_display" -> Items.GRASS_BLOCK;
            case "text_display" -> Items.OAK_SIGN;
            case "item" -> Items.DIAMOND;
            case "llama_spit" -> Items.LEAD;
            case "mannequin" -> Items.ARMOR_STAND;
            default -> null;
        };
    }

    private static Item itemByName(String path) {
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", path));
            if (item != null && item != Items.AIR) return item;
        } catch (Throwable ignored) {}
        return null;
    }
}

