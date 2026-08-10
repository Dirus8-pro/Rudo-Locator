package com.kompi.orelocator.client;

import com.kompi.orelocator.OreLocatorConfig;
import com.kompi.orelocator.client.OreFilterHolder;
import com.kompi.orelocator.init.ModItems;
import com.kompi.orelocator.network.ModNetwork;
import com.kompi.orelocator.network.SyncOreFilterPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class OreFilterScreen extends Screen {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("rudo-locator", "textures/gui/ore_filter_gui.png");
    private static final ResourceLocation GUI_TEXTURE_RGB = new ResourceLocation("rudo-locator", "textures/gui/ore_filter_gui_rgb.png");

    private final ItemStack locatorStack;
    private final Map<String, Boolean> filterState = new LinkedHashMap<>();
    private final Map<String, Integer> oreColors = new HashMap<>();
    private final List<OreEntry> oreEntries = new ArrayList<>();

    private final int guiWidth = 256;
    private final int guiHeight = 195;
    private int leftPos, topPos;

    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private static final int VISIBLE_ICONS = 9;
    private static final int ICON_SIZE = 20;
    private static final int ICON_SPACING = 3;

    private boolean isDraggingOreScroll = false;
    private final int scrollBarWidth = 204;
    private final int scrollThumbWidth = 16;

    private boolean colorPanelOpen = false;
    private int rVal = 0, gVal = 255, bVal = 0;
    private boolean isDraggingSlider = false;
    private int activeSlider = -1;

    private final int sliderWidth = 105;

    private int currentRadius;
    private int maxRadius;
    private EditBox radiusEdit;

    private static final Map<String, Integer> DEFAULT_ORE_COLORS = new HashMap<>();
    static {
        DEFAULT_ORE_COLORS.put("iron",       0xFFD9B382);
        DEFAULT_ORE_COLORS.put("copper",     0xFFFF8020);
        DEFAULT_ORE_COLORS.put("gold",       0xFFFFFF00);
        DEFAULT_ORE_COLORS.put("coal",       0xFF1A1A1A);
        DEFAULT_ORE_COLORS.put("redstone",   0xFFFF0000);
        DEFAULT_ORE_COLORS.put("lapis",      0xFF0033FF);
        DEFAULT_ORE_COLORS.put("diamond",    0xFF00FFFF);
        DEFAULT_ORE_COLORS.put("emerald",    0xFF00FF00);
        DEFAULT_ORE_COLORS.put("netherite",  0xFFCC00FF);
        DEFAULT_ORE_COLORS.put("quartz",     0xFFE6E6E6);
        DEFAULT_ORE_COLORS.put("nether_gold",0xFFFFCC00);
    }

    private static final Map<String, Component> ORE_NAMES = new HashMap<>();
    static {
        ORE_NAMES.put("iron",        Component.translatable("rudo-locator.ore.iron"));
        ORE_NAMES.put("copper",      Component.translatable("rudo-locator.ore.copper"));
        ORE_NAMES.put("gold",        Component.translatable("rudo-locator.ore.gold"));
        ORE_NAMES.put("coal",        Component.translatable("rudo-locator.ore.coal"));
        ORE_NAMES.put("redstone",    Component.translatable("rudo-locator.ore.redstone"));
        ORE_NAMES.put("lapis",       Component.translatable("rudo-locator.ore.lapis"));
        ORE_NAMES.put("diamond",     Component.translatable("rudo-locator.ore.diamond"));
        ORE_NAMES.put("emerald",     Component.translatable("rudo-locator.ore.emerald"));
        ORE_NAMES.put("netherite",   Component.translatable("rudo-locator.ore.netherite"));
        ORE_NAMES.put("quartz",      Component.translatable("rudo-locator.ore.quartz"));
        ORE_NAMES.put("nether_gold", Component.translatable("rudo-locator.ore.nether_gold"));
    }

    private static final Map<String, List<Component>> ORE_INFO = new HashMap<>();
    static {
        for (String ore : ORE_NAMES.keySet()) {
            List<Component> lines = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                lines.add(Component.translatable("rudo-locator.info." + ore + "." + i));
            }
            ORE_INFO.put(ore, lines);
        }
    }

    private static final String[] VANILLA_ORDER = {
            "iron", "copper", "gold", "coal", "redstone", "lapis", "diamond", "emerald", "netherite", "quartz", "nether_gold"
    };

    public OreFilterScreen(ItemStack stack, CompoundTag currentFilter) {
        super(Component.translatable("screen.rudo-locator.ore_filter"));
        this.locatorStack = stack;

        OreLocatorConfig config = OreLocatorConfig.getInstance();
        Item item = stack.getItem();
        if (item == ModItems.COPPER_ORE_LOCATOR) {
            currentRadius = config.oreLocatorRadius.copperCurrentRadius;
            maxRadius = config.oreLocatorRadius.copperMaxRadius;
        } else if (item == ModItems.IRON_ORE_LOCATOR) {
            currentRadius = config.oreLocatorRadius.ironCurrentRadius;
            maxRadius = config.oreLocatorRadius.ironMaxRadius;
        } else if (item == ModItems.GOLD_ORE_LOCATOR) {
            currentRadius = config.oreLocatorRadius.goldCurrentRadius;
            maxRadius = config.oreLocatorRadius.goldMaxRadius;
        } else if (item == ModItems.DIAMOND_ORE_LOCATOR) {
            currentRadius = config.oreLocatorRadius.diamondCurrentRadius;
            maxRadius = config.oreLocatorRadius.diamondMaxRadius;
        } else if (item == ModItems.NETHERITE_ORE_LOCATOR) {
            currentRadius = config.oreLocatorRadius.netheriteCurrentRadius;
            maxRadius = config.oreLocatorRadius.netheriteMaxRadius;
        } else {
            currentRadius = 64;
            maxRadius = 64;
        }

        Map<String, String> groupToId = new LinkedHashMap<>();
        for (String g : VANILLA_ORDER) groupToId.put(g, null);

        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null) {
                boolean isOre = block.defaultBlockState().is(ConventionalBlockTags.ORES)
                        || id.getPath().endsWith("_ore")
                        || block == Blocks.ANCIENT_DEBRIS;
                if (isOre) {
                    String group = getOreGroup(block.defaultBlockState());
                    if (group != null) {
                        if (groupToId.containsKey(group)) {
                            if (groupToId.get(group) == null) {
                                groupToId.put(group, id.toString());
                            }
                        } else {
                            groupToId.put(group, id.toString());
                        }
                    }
                }
            }
        }

        for (String key : groupToId.keySet()) {
            filterState.put(key, !currentFilter.contains(key) || currentFilter.getBoolean(key));
            int defaultColor = currentFilter.contains(key + "_rgb_color") ? currentFilter.getInt(key + "_rgb_color") : DEFAULT_ORE_COLORS.getOrDefault(key, 0xFFFF0080);
            oreColors.put(key, defaultColor);
        }

        for (String key : groupToId.keySet()) {
            String iconId = groupToId.get(key);
            if (iconId != null) {
                Component displayName = ORE_NAMES.getOrDefault(key, null);
                if (displayName == null) {
                    Block iconBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation(iconId));
                    displayName = iconBlock != null ? iconBlock.getName() : Component.literal(key);
                }
                oreEntries.add(new OreEntry(key, iconId, displayName));
            }
        }

        if (!oreEntries.isEmpty()) {
            updateSlidersFromCurrentOre();
        }
    }

    private void updateSlidersFromCurrentOre() {
        String currentKey = oreEntries.get(selectedIndex).key;
        int color = oreColors.getOrDefault(currentKey, 0x00FF00);
        this.rVal = (color >> 16) & 0xFF;
        this.gVal = (color >> 8) & 0xFF;
        this.bVal = color & 0xFF;
    }

    private String getOreGroup(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return "iron";
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return "copper";
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) return "gold";
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return "coal";
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return "redstone";
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return "lapis";
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return "diamond";
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return "emerald";
        if (block == Blocks.ANCIENT_DEBRIS) return "netherite";
        if (block == Blocks.NETHER_QUARTZ_ORE) return "quartz";
        if (block == Blocks.NETHER_GOLD_ORE) return "nether_gold";
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null ? id.toString() : null;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;
        this.clearWidgets();

        int fieldWidth = 40;
        int fieldHeight = 12;
        int fieldX = leftPos + 125;
        int fieldY = topPos + guiHeight - 65;

        // Прозрачное поле ввода радиуса (без чёрного фона)
        this.radiusEdit = new EditBox(this.font, fieldX, fieldY, fieldWidth, fieldHeight, Component.literal("")) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                if (this.isVisible()) {
                    // Используем шрифт из экрана (Minecraft.getInstance().font)
                    net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
                    int textColor = 0xFFFFFF; // всегда белый, т.к. поле всегда доступно
                    int x = this.getX() + 4;
                    int y = this.getY() + (this.height - 8) / 2;
                    graphics.drawString(font, this.getValue(), x, y, textColor, false);

                    if (this.isFocused()) {
                        int cursorPos = this.getCursorPosition();
                        String text = this.getValue();
                        int cursorX = x + font.width(text.substring(0, cursorPos));
                        graphics.fill(cursorX, y - 1, cursorX + 1, y + 9, 0xFFCCCCCC);
                    }
                }
            }
        };

        this.radiusEdit.setValue(String.valueOf(currentRadius));
        this.radiusEdit.setFilter(s -> s.matches("\\d{0,3}"));
        this.radiusEdit.setResponder(text -> {
            if (!text.isEmpty()) {
                try {
                    int val = Integer.parseInt(text);
                    if (val < 0) val = 0;
                    if (val > maxRadius) val = maxRadius;
                    currentRadius = val;

                    // Сохраняем в конфиг
                    OreLocatorConfig config = OreLocatorConfig.getInstance();
                    Item item = locatorStack.getItem();

                    if (item == ModItems.COPPER_ORE_LOCATOR) {
                        config.oreLocatorRadius.copperCurrentRadius = currentRadius;
                    } else if (item == ModItems.IRON_ORE_LOCATOR) {
                        config.oreLocatorRadius.ironCurrentRadius = currentRadius;
                    } else if (item == ModItems.GOLD_ORE_LOCATOR) {
                        config.oreLocatorRadius.goldCurrentRadius = currentRadius;
                    } else if (item == ModItems.DIAMOND_ORE_LOCATOR) {
                        config.oreLocatorRadius.diamondCurrentRadius = currentRadius;
                    } else if (item == ModItems.NETHERITE_ORE_LOCATOR) {
                        config.oreLocatorRadius.netheriteCurrentRadius = currentRadius;
                    }

                    OreLocatorConfig.save();

                } catch (NumberFormatException ignored) {}
            } else {
                currentRadius = 0;
            }
        });

        this.addRenderableWidget(radiusEdit);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= topPos + 12 && mouseY <= topPos + 12 + ICON_SIZE) {
            if (delta < 0 && scrollOffset < oreEntries.size() - VISIBLE_ICONS) scrollOffset++;
            else if (delta > 0 && scrollOffset > 0) scrollOffset--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startX = leftPos + (guiWidth - (VISIBLE_ICONS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING)) / 2;
            int startY = topPos + 12;

            for (int i = 0; i < VISIBLE_ICONS; i++) {
                int entryIndex = scrollOffset + i;
                if (entryIndex >= oreEntries.size()) break;
                int iconX = startX + i * (ICON_SIZE + ICON_SPACING);
                if (mouseX >= iconX && mouseX <= iconX + ICON_SIZE && mouseY >= startY && mouseY <= startY + ICON_SIZE) {
                    this.selectedIndex = entryIndex;
                    this.colorPanelOpen = false;
                    updateSlidersFromCurrentOre();
                    playClickSound();
                    return true;
                }
            }

            int oreScrollX = leftPos + 26;
            int oreScrollY = topPos + 36;
            if (mouseX >= oreScrollX && mouseX <= oreScrollX + scrollBarWidth && mouseY >= oreScrollY && mouseY <= oreScrollY + 5) {
                this.isDraggingOreScroll = true;
                updateOreScroll(mouseX - oreScrollX);
                return true;
            }

            if (!oreEntries.isEmpty()) {
                OreEntry current = oreEntries.get(selectedIndex);
                int textX = leftPos + 85;

                if (!colorPanelOpen && mouseX >= leftPos + 20 && mouseX <= leftPos + 65 && mouseY >= topPos + 115 && mouseY <= topPos + 126) {
                    filterState.put(current.key, !filterState.get(current.key));
                    playClickSound();
                    return true;
                }

                if (!colorPanelOpen && mouseX >= leftPos + 185 && mouseX <= leftPos + 250 && mouseY >= topPos + 115 && mouseY <= topPos + 126) {
                    boolean anyDisabled = filterState.values().stream().anyMatch(val -> !val);
                    filterState.keySet().forEach(key -> filterState.put(key, anyDisabled));
                    playClickSound();
                    return true;
                }

                int totalBottomWidth = 90 + 6 + 10 + 8 + 45;
                int bottomStartX = leftPos + (guiWidth - totalBottomWidth) / 2;

                if (mouseX >= bottomStartX && mouseX <= bottomStartX + 90 && mouseY >= topPos + 150 && mouseY <= topPos + 161) {
                    this.colorPanelOpen = !this.colorPanelOpen;
                    if (colorPanelOpen) updateSlidersFromCurrentOre();
                    playClickSound();
                    return true;
                }

                if (mouseX >= bottomStartX + 90 + 6 + 10 + 8 && mouseX <= bottomStartX + 90 + 6 + 10 + 8 + 45 && mouseY >= topPos + 150 && mouseY <= topPos + 161) {
                    oreColors.put(current.key, DEFAULT_ORE_COLORS.getOrDefault(current.key, 0xFFFF0080));
                    updateSlidersFromCurrentOre();
                    playClickSound();
                    return true;
                }

                if (colorPanelOpen) {
                    int sliderStartX = textX + 16;
                    for (int i = 0; i < 3; i++) {
                        int sY = topPos + 70 + (i * 12);
                        if (mouseX >= sliderStartX && mouseX <= sliderStartX + sliderWidth && mouseY >= sY - 2 && mouseY <= sY + 8) {
                            this.isDraggingSlider = true;
                            this.activeSlider = i;
                            updateSliderValue((int) mouseX - sliderStartX);
                            return true;
                        }
                    }

                    int applyLeft = textX + 16;
                    int applyRight = applyLeft + sliderWidth;
                    if (mouseX >= applyLeft && mouseX <= applyRight && mouseY >= topPos + 112 && mouseY <= topPos + 128) {
                        int combinedColor = (rVal << 16) | (gVal << 8) | bVal;
                        oreColors.put(current.key, combinedColor);
                        this.colorPanelOpen = false;
                        playClickSound();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClickSound() {
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingSlider = false;
            this.isDraggingOreScroll = false;
            this.activeSlider = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingOreScroll) {
            updateOreScroll(mouseX - (leftPos + 26));
            return true;
        }
        if (this.isDraggingSlider && this.activeSlider != -1) {
            int textX = leftPos + 85;
            updateSliderValue((int) mouseX - (textX + 16));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateOreScroll(double relativeX) {
        int maxScrollable = oreEntries.size() - VISIBLE_ICONS;
        if (maxScrollable <= 0) return;
        double percentage = (relativeX - (scrollThumbWidth / 2.0)) / (scrollBarWidth - scrollThumbWidth);
        percentage = Math.max(0.0, Math.min(1.0, percentage));
        this.scrollOffset = (int) Math.round(percentage * maxScrollable);
    }

    private void updateSliderValue(int relativeX) {
        int val = Math.max(0, Math.min(255, (relativeX * 255) / sliderWidth));
        if (activeSlider == 0) rVal = val;
        else if (activeSlider == 1) gVal = val;
        else if (activeSlider == 2) bVal = val;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        ResourceLocation currentTexture = colorPanelOpen ? GUI_TEXTURE_RGB : GUI_TEXTURE;
        graphics.blit(currentTexture, leftPos, topPos, 0, 0, 256, guiHeight, 256, 256);

        int startX = leftPos + (guiWidth - (VISIBLE_ICONS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING)) / 2;
        int startY = topPos + 12;

        for (int i = 0; i < VISIBLE_ICONS; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= oreEntries.size()) break;

            OreEntry entry = oreEntries.get(entryIndex);
            int iconX = startX + i * (ICON_SIZE + ICON_SPACING) + 1;

            if (entryIndex == selectedIndex) {
                graphics.blit(currentTexture, iconX - 2, startY - 2, 0, 220, 22, 22, 256, 256);
            }

            graphics.pose().pushPose();
            graphics.pose().translate(iconX + 2.3, startY + 2, 0);
            graphics.pose().scale(0.85F, 0.85F, 1.0F);
            graphics.renderItem(entry.getIconStack(), 0, 0);
            graphics.pose().popPose();

            if (!filterState.getOrDefault(entry.key, true)) {
                graphics.blit(GUI_TEXTURE, iconX, startY, 24, 220, 20, 20, 256, 256);
            }
        }

        int oreScrollX = leftPos + 26;
        int oreScrollY = topPos + 36;
        int maxScrollable = oreEntries.size() - VISIBLE_ICONS;
        int thumbXOffset = 0;
        if (maxScrollable > 0) {
            thumbXOffset = (scrollOffset * (scrollBarWidth - scrollThumbWidth)) / maxScrollable;
        }
        graphics.fill(oreScrollX + thumbXOffset, oreScrollY,
                oreScrollX + thumbXOffset + scrollThumbWidth, oreScrollY + 4, 0xFF8B8B8B);

        if (!oreEntries.isEmpty()) {
            OreEntry current = oreEntries.get(selectedIndex);

            graphics.pose().pushPose();
            graphics.pose().translate(leftPos + 18, topPos + 51, 0);
            graphics.pose().scale(3.3F, 3.3F, 1.0F);
            graphics.renderItem(current.getIconStack(), 0, 0);
            graphics.pose().popPose();

            int textX = leftPos + 85;
            graphics.drawString(this.font, current.displayName, textX, topPos + 56, 0xFFFFFF, false);

            boolean isEnabled = filterState.getOrDefault(current.key, true);

            if (!colorPanelOpen) {
                Component statusText = isEnabled
                        ? Component.translatable("rudo-locator.status.active")
                        : Component.translatable("rudo-locator.status.inactive");
                int statusColor = isEnabled ? 0xFF00FF00 : 0xFFFF0000;
                graphics.drawString(this.font, statusText, textX, topPos + 66, statusColor, false);

                graphics.pose().pushPose();
                graphics.pose().translate(textX, topPos + 78, 0);
                graphics.pose().scale(0.85F, 0.85F, 1.0F);
                List<Component> infoLines = ORE_INFO.getOrDefault(current.key,
                        Collections.singletonList(Component.translatable("rudo-locator.info.unknown")));
                for (int i = 0; i < infoLines.size(); i++) {
                    graphics.drawString(this.font, infoLines.get(i), 0, i * 10, 0x999999, false);
                }
                graphics.pose().popPose();

                Component toggleText = isEnabled
                        ? Component.translatable("rudo-locator.button.toggle_off")
                        : Component.translatable("rudo-locator.button.toggle_on");
                int toggleColor = isEnabled ? 0xFFFF5555 : 0xFF55FF55;
                graphics.drawString(this.font, toggleText, textX - 65, topPos + 115, toggleColor, false);

                boolean anyDisabled = filterState.values().stream().anyMatch(val -> !val);
                Component toggleAllText = anyDisabled
                        ? Component.translatable("rudo-locator.button.toggle_all_on")
                        : Component.translatable("rudo-locator.button.toggle_all_off");
                graphics.drawString(this.font, toggleAllText, textX + 100, topPos + 115, 0xFFFFFF, false);

            } else {
                int sliderStartX = textX + 16;
                Component applyText = Component.translatable("rudo-locator.button.apply");
                int applyWidth = this.font.width(applyText);
                int applyX = sliderStartX + (sliderWidth / 2) - (applyWidth / 2);
                graphics.drawString(this.font, applyText, applyX - 38, topPos + 116, 0xFF55FF55, false);

                int[] sliderVals = {rVal, gVal, bVal};
                int[] sliderColors = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
                Component[] sliderLabels = {
                        Component.translatable("rudo-locator.slider.red"),
                        Component.translatable("rudo-locator.slider.green"),
                        Component.translatable("rudo-locator.slider.blue")
                };

                for (int i = 0; i < 3; i++) {
                    int sY = topPos + 70 + (i * 12);
                    graphics.drawString(this.font, sliderLabels[i], textX, sY, sliderColors[i], false);
                    graphics.fill(sliderStartX, sY + 4, sliderStartX + sliderWidth, sY + 5, 0xFF3F3F3F);
                    int handleX = sliderStartX + (sliderVals[i] * sliderWidth) / 255;
                    graphics.fill(handleX - 1, sY + 1, handleX + 2, sY + 8, sliderColors[i]);
                    graphics.drawString(this.font, String.valueOf(sliderVals[i]),
                            sliderStartX + sliderWidth + 12, sY, 0xFFFFFF, false);
                }
            }

            int totalBottomWidth = 90 + 6 + 10 + 8 + 45;
            int bottomStartX = leftPos + (guiWidth - totalBottomWidth) / 2;
            int bottomY = topPos + 150;

            int colorLinkColor = colorPanelOpen ? 0xFFFFAA00 : 0xFF55FFFF;
            graphics.drawString(this.font, Component.translatable("rudo-locator.button.change_color"),
                    bottomStartX, bottomY, colorLinkColor, false);

            int savedColor = oreColors.getOrDefault(current.key, 0x00FF00);
            int currentCombinedColor = (255 << 24) | (colorPanelOpen ? ((rVal << 16) | (gVal << 8) | bVal) : savedColor);
            int squareX = bottomStartX + 90 + 6;
            graphics.fill(squareX, bottomY - 1, squareX + 10, bottomY + 9, currentCombinedColor);

            int resetX = squareX + 10 + 8;
            graphics.drawString(this.font, Component.translatable("rudo-locator.button.reset"),
                    resetX, bottomY, 0xAAAAAA, false);
        }

        graphics.drawString(this.font, Component.translatable("rudo-locator.gui.radius"),
                leftPos + 85, topPos + guiHeight - 63, 0xFFFFFF, false);
        radiusEdit.render(graphics, mouseX, mouseY, partialTick);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        CompoundTag oreFilter = new CompoundTag();
        for (String key : filterState.keySet()) {
            oreFilter.putBoolean(key, filterState.get(key));
        }
        for (String key : oreColors.keySet()) {
            oreFilter.putInt(key + "_rgb_color", oreColors.get(key));
        }
        if (Minecraft.getInstance().player != null) {
            OreFilterHolder.setFilter(oreFilter);
        }
        // Отправка на сервер через Fabric
        SyncOreFilterPacket packet = new SyncOreFilterPacket(oreFilter);
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        packet.encode(buf);
        ClientPlayNetworking.send(ModNetwork.SYNC_ORE_FILTER_PACKET, buf);
        super.onClose();
    }

    private static class OreEntry {
        final String key;
        final String iconBlockId;
        final Component displayName;
        private ItemStack cachedStack = null;

        OreEntry(String key, String iconBlockId, Component displayName) {
            this.key = key;
            this.iconBlockId = iconBlockId;
            this.displayName = displayName;
        }

        ItemStack getIconStack() {
            if (cachedStack == null) {
                Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(iconBlockId));
                cachedStack = (block != null) ? new ItemStack(block.asItem()) : new ItemStack(Blocks.STONE);
            }
            return cachedStack;
        }
    }
}