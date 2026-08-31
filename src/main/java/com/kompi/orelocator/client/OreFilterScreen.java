package com.kompi.orelocator.client;

import net.minecraft.world.level.block.Block;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.kompi.orelocator.Config;
import com.kompi.orelocator.init.ModItems;
import com.kompi.orelocator.network.ModNetwork;
import com.kompi.orelocator.network.SyncOreFilterPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class OreFilterScreen extends Screen {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("orelocator", "textures/gui/ore_filter_gui.png");
    private static final ResourceLocation GUI_TEXTURE_RGB = new ResourceLocation("orelocator", "textures/gui/ore_filter_gui_rgb.png");

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
    // растояние межд верхними блоками их срабатывания
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
        ORE_NAMES.put("iron",        Component.translatable("orelocator.ore.iron"));
        ORE_NAMES.put("copper",      Component.translatable("orelocator.ore.copper"));
        ORE_NAMES.put("gold",        Component.translatable("orelocator.ore.gold"));
        ORE_NAMES.put("coal",        Component.translatable("orelocator.ore.coal"));
        ORE_NAMES.put("redstone",    Component.translatable("orelocator.ore.redstone"));
        ORE_NAMES.put("lapis",       Component.translatable("orelocator.ore.lapis"));
        ORE_NAMES.put("diamond",     Component.translatable("orelocator.ore.diamond"));
        ORE_NAMES.put("emerald",     Component.translatable("orelocator.ore.emerald"));
        ORE_NAMES.put("netherite",   Component.translatable("orelocator.ore.netherite"));
        ORE_NAMES.put("quartz",      Component.translatable("orelocator.ore.quartz"));
        ORE_NAMES.put("nether_gold", Component.translatable("orelocator.ore.nether_gold"));
    }

    private static final Map<String, List<Component>> ORE_INFO = new HashMap<>();
    static {
        for (String ore : ORE_NAMES.keySet()) {
            List<Component> lines = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                lines.add(Component.translatable("orelocator.info." + ore + "." + i));
            }
            ORE_INFO.put(ore, lines);
        }
    }

    private static final String[] VANILLA_ORDER = {
            "iron", "copper", "gold", "coal", "redstone", "lapis", "diamond", "emerald", "netherite", "quartz", "nether_gold"
    };

    // Безопасное получение иконок блоков руд напрямую из Реестра Forge
    private static Item getOreBlockItem(String key) {
        ResourceLocation rl = switch (key) {
            case "iron" -> new ResourceLocation("minecraft", "iron_ore");
            case "copper" -> new ResourceLocation("minecraft", "copper_ore");
            case "gold" -> new ResourceLocation("minecraft", "gold_ore");
            case "coal" -> new ResourceLocation("minecraft", "coal_ore");
            case "redstone" -> new ResourceLocation("minecraft", "redstone_ore");
            case "lapis" -> new ResourceLocation("minecraft", "lapis_ore");
            case "diamond" -> new ResourceLocation("minecraft", "diamond_ore");
            case "emerald" -> new ResourceLocation("minecraft", "emerald_ore");
            case "netherite" -> new ResourceLocation("minecraft", "ancient_debris");
            case "quartz" -> new ResourceLocation("minecraft", "nether_quartz_ore");
            case "nether_gold" -> new ResourceLocation("minecraft", "nether_gold_ore");
            default -> new ResourceLocation("minecraft", "stone");
        };
        Item found = ForgeRegistries.ITEMS.getValue(rl);
        return (found != null && found != Items.AIR) ? found : Blocks.STONE.asItem();
    }

    public OreFilterScreen(ItemStack stack, CompoundTag currentFilter) {
        super(Component.translatable("screen.orelocator.ore_filter"));
        this.locatorStack = stack;

        Item item = stack.getItem();
        if (item == ModItems.COPPER_ORE_LOCATOR.get()) {
            currentRadius = Config.COPPER_CURRENT_RADIUS.get();
            maxRadius = Config.COPPER_MAX_RADIUS.get();
        } else if (item == ModItems.IRON_ORE_LOCATOR.get()) {
            currentRadius = Config.IRON_CURRENT_RADIUS.get();
            maxRadius = Config.IRON_MAX_RADIUS.get();
        } else if (item == ModItems.GOLD_ORE_LOCATOR.get()) {
            currentRadius = Config.GOLD_CURRENT_RADIUS.get();
            maxRadius = Config.GOLD_MAX_RADIUS.get();
        } else if (item == ModItems.DIAMOND_ORE_LOCATOR.get()) {
            currentRadius = Config.DIAMOND_CURRENT_RADIUS.get();
            maxRadius = Config.DIAMOND_MAX_RADIUS.get();
        } else if (item == ModItems.NETHERITE_ORE_LOCATOR.get()) {
            currentRadius = Config.NETHERITE_CURRENT_RADIUS.get();
            maxRadius = Config.NETHERITE_MAX_RADIUS.get();
        } else {
            currentRadius = 64;
            maxRadius = 64;
        }

        for (String key : VANILLA_ORDER) {
            filterState.put(key, !currentFilter.contains(key) || currentFilter.getBoolean(key));
            int defaultColor = currentFilter.contains(key + "_rgb_color") ? currentFilter.getInt(key + "_rgb_color") : DEFAULT_ORE_COLORS.getOrDefault(key, 0xFFFF0080);
            oreColors.put(key, defaultColor);

            Component displayName = ORE_NAMES.getOrDefault(key, Component.literal(key));
            oreEntries.add(new OreEntry(key, displayName, new ItemStack(getOreBlockItem(key))));
        }

        // --- НОВЫЙ КОД: Динамический поиск модовых руд в реестре ---
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            if (id != null && !id.getNamespace().equals("minecraft")) {
                String path = id.getPath().toLowerCase();
                String desc = block.getDescriptionId().toLowerCase();

                if (path.contains("ore") || path.contains("debris") || desc.contains("ore")) {
                    String key = id.toString();

                    if (!filterState.containsKey(key)) {
                        filterState.put(key, !currentFilter.contains(key) || currentFilter.getBoolean(key));

                        // Все модовые руды получают одинаковый розовый цвет по умолчанию
                        int defaultColor = currentFilter.contains(key + "_rgb_color") ?
                                currentFilter.getInt(key + "_rgb_color") : 0xFFFF0080;
                        oreColors.put(key, defaultColor);

                        Component displayName = Component.translatable(block.getDescriptionId());
                        oreEntries.add(new OreEntry(key, displayName, new ItemStack(block.asItem())));
                    }
                }
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

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;
        this.clearWidgets();

        int fieldWidth = 35;
        int fieldHeight = 12;
        int fieldX = leftPos + 126;
        int fieldY = topPos + 132;

        this.radiusEdit = new EditBox(this.font, fieldX, fieldY, fieldWidth, fieldHeight, Component.literal("")) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                if (!this.visible) return;

                Font font = Minecraft.getInstance().font;
                font.draw(poseStack, this.getValue(), this.x, this.y, 0xFFFFFF);

                if (this.isFocused()) {
                    int cursorPos = this.getCursorPosition();
                    String text = this.getValue();
                    int cursorX = this.x + font.width(text.substring(0, cursorPos));
                    fill(poseStack, cursorX, this.y - 1, cursorX + 1, this.y + 9, 0xFFCCCCCC);
                }
            }
        };

        this.radiusEdit.setBordered(false);
        this.radiusEdit.setValue(String.valueOf(currentRadius));
        this.radiusEdit.setFilter(s -> s.matches("\\d{0,3}"));
        this.radiusEdit.setResponder(text -> {
            if (!text.isEmpty()) {
                try {
                    int val = Integer.parseInt(text);
                    if (val < 0) val = 0;
                    if (val > maxRadius) val = maxRadius;
                    currentRadius = val;

                    Item item = locatorStack.getItem();
                    if (item == ModItems.COPPER_ORE_LOCATOR.get()) {
                        Config.setCopperRadius(val);
                    } else if (item == ModItems.IRON_ORE_LOCATOR.get()) {
                        Config.setIronRadius(val);
                    } else if (item == ModItems.GOLD_ORE_LOCATOR.get()) {
                        Config.setGoldRadius(val);
                    } else if (item == ModItems.DIAMOND_ORE_LOCATOR.get()) {
                        Config.setDiamondRadius(val);
                    } else if (item == ModItems.NETHERITE_ORE_LOCATOR.get()) {
                        Config.setNetheriteRadius(val);
                    }
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

                if (mouseX >= bottomStartX && mouseX <= bottomStartX + 90 && mouseY >= topPos + 150 && mouseY <= topPos + 160) {
                    this.colorPanelOpen = !this.colorPanelOpen;
                    if (colorPanelOpen) updateSlidersFromCurrentOre();
                    playClickSound();
                    return true;
                }

                if (mouseX >= bottomStartX + 90 + 6 + 10 + 8 && mouseX <= bottomStartX + 90 + 6 + 10 + 8 + 45 && mouseY >= topPos + 150 && mouseY <= topPos + 160) {
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
        if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
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
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        ResourceLocation currentTexture = colorPanelOpen ? GUI_TEXTURE_RGB : GUI_TEXTURE;
        RenderSystem.setShaderTexture(0, currentTexture);
        this.blit(poseStack, leftPos, topPos, 0, 0, 256, guiHeight);

        int startX = leftPos + (guiWidth - (VISIBLE_ICONS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING)) / 2;
        int startY = topPos + 12;

        // Отрисовка верхнего ряда иконок
        for (int i = 0; i < VISIBLE_ICONS; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= oreEntries.size()) break;

            OreEntry entry = oreEntries.get(entryIndex);
            int iconX = startX + i * (ICON_SIZE + ICON_SPACING);

            // ВАЖНО: Возвращаем нашу текстуру GUI, так как renderGuiItem сбил её на атлас блоков
            RenderSystem.setShaderTexture(0, currentTexture);

            // Рисуем бирюзовую рамку выделения (координаты 0, 220)
            if (entryIndex == selectedIndex) {
                this.blit(poseStack, iconX - 1, startY - 2, 0, 220, 22, 22);
            }

            // Уменьшаем иконку до 14x14 и центрируем в слоте 20x20
            PoseStack ps = RenderSystem.getModelViewStack();
            ps.pushPose();
            ps.translate(iconX + 2 + 1, startY + 2, 0); // +1 для центрирования
            ps.scale(0.880F, 0.880F, 1.0F);                // 14/16 = 0.875
            this.itemRenderer.renderGuiItem(entry.getIconStack(), 0, 0);
            ps.popPose();
            RenderSystem.applyModelViewMatrix();

            // Если руда выключена - рисуем поверх красную рамку отключения
            if (!filterState.getOrDefault(entry.key, true)) {
                // Снова возвращаем текстуру GUI
                RenderSystem.setShaderTexture(0, GUI_TEXTURE);
                this.blit(poseStack, iconX + 1, startY, 24, 220, 20, 20);
                RenderSystem.setShaderTexture(0, currentTexture);
            }
        }

// Восстанавливаем текстуру GUI для рендера остальной части окна
        RenderSystem.setShaderTexture(0, currentTexture);

        int oreScrollX = leftPos + 26;
        int oreScrollY = topPos + 36;
        int maxScrollable = oreEntries.size() - VISIBLE_ICONS;
        int thumbXOffset = 0;
        if (maxScrollable > 0) {
            thumbXOffset = (scrollOffset * (scrollBarWidth - scrollThumbWidth)) / maxScrollable;
        }
        fill(poseStack, oreScrollX + thumbXOffset, oreScrollY,
                oreScrollX + thumbXOffset + scrollThumbWidth, oreScrollY + 4, 0xFF8B8B8B);

        if (!oreEntries.isEmpty()) {
            OreEntry current = oreEntries.get(selectedIndex);

            // Правильный рендер крупной иконки руды без улетания в левый верхний угол экрана
            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushPose();

            // Сдвигаем глобальную матрицу в нужное место (внутрь рамки интерфейса)
            modelViewStack.translate(leftPos + 26, topPos + 54, 0);
            modelViewStack.scale(3.5F, 3.5F, 1.0F); // Увеличиваем в 2.3 раза

            // Применяем изменения матрицы к RenderSystem перед рендером предмета
            RenderSystem.applyModelViewMatrix();

            // Теперь рендерим предмет в координатах 0, 0 (он отрендерится там, куда мы сдвинули матрицу)
            this.itemRenderer.renderGuiItem(current.getIconStack(), -3, -1);

            // Обязательно возвращаем матрицу в исходное состояние, чтобы не сломать остальной интерфейс
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();

            int textX = leftPos + 85;
            this.font.draw(poseStack, current.displayName, textX, topPos + 56, 0xFFFFFF);


            boolean isEnabled = filterState.getOrDefault(current.key, true);

            if (!colorPanelOpen) {
                Component statusText = isEnabled
                        ? Component.translatable("orelocator.status.active")
                        : Component.translatable("orelocator.status.inactive");
                int statusColor = isEnabled ? 0xFF00FF00 : 0xFFFF0000;
                this.font.draw(poseStack, statusText, textX, topPos + 66, statusColor);

                poseStack.pushPose();
                poseStack.translate(textX, topPos + 78, 0);
                poseStack.scale(0.85F, 0.85F, 1.0F);
                List<Component> infoLines = ORE_INFO.getOrDefault(current.key,
                        Collections.singletonList(Component.translatable("orelocator.info.unknown")));
                for (int i = 0; i < infoLines.size(); i++) {
                    this.font.draw(poseStack, infoLines.get(i), 0, i * 10, 0x999999);
                }
                poseStack.popPose();

                Component toggleText = isEnabled
                        ? Component.translatable("orelocator.button.toggle_off")
                        : Component.translatable("orelocator.button.toggle_on");
                int toggleColor = isEnabled ? 0xFFFF5555 : 0xFF55FF55;
                this.font.draw(poseStack, toggleText, textX - 65, topPos + 115, toggleColor);

                boolean anyDisabled = filterState.values().stream().anyMatch(val -> !val);
                Component toggleAllText = anyDisabled
                        ? Component.translatable("orelocator.button.toggle_all_on")
                        : Component.translatable("orelocator.button.toggle_all_off");
                this.font.draw(poseStack, toggleAllText, textX + 100, topPos + 115, 0xFFFFFF);

            } else {
                int sliderStartX = textX + 16;
                Component applyText = Component.translatable("orelocator.button.apply");
                int applyWidth = this.font.width(applyText);
                int applyX = sliderStartX + (sliderWidth / 2) - (applyWidth / 2);
                this.font.draw(poseStack, applyText, applyX - 38, topPos + 115, 0xFF55FF55);

                int[] sliderVals = {rVal, gVal, bVal};
                int[] sliderColors = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
                Component[] sliderLabels = {
                        Component.translatable("orelocator.slider.red"),
                        Component.translatable("orelocator.slider.green"),
                        Component.translatable("orelocator.slider.blue")
                };

                for (int i = 0; i < 3; i++) {
                    int sY = topPos + 70 + (i * 12);
                    this.font.draw(poseStack, sliderLabels[i], textX, sY, sliderColors[i]);
                    fill(poseStack, sliderStartX, sY + 4, sliderStartX + sliderWidth, sY + 5, 0xFF3F3F3F);
                    int handleX = sliderStartX + (sliderVals[i] * sliderWidth) / 255;
                    fill(poseStack, handleX - 1, sY + 1, handleX + 2, sY + 8, sliderColors[i]);
                    this.font.draw(poseStack, String.valueOf(sliderVals[i]),
                            sliderStartX + sliderWidth + 12, sY, 0xFFFFFF);
                }
            }

            int totalBottomWidth = 90 + 6 + 10 + 8 + 45;
            int bottomStartX = leftPos + (guiWidth - totalBottomWidth) / 2;
            int bottomY = topPos + 150;

            int colorLinkColor = colorPanelOpen ? 0xFFFFAA00 : 0xFF55FFFF;
            this.font.draw(poseStack, Component.translatable("orelocator.button.change_color"),
                    bottomStartX, bottomY, colorLinkColor);

            int savedColor = oreColors.getOrDefault(current.key, 0x00FF00);
            int currentCombinedColor = (255 << 24) | (colorPanelOpen ? ((rVal << 16) | (gVal << 8) | bVal) : savedColor);
            int squareX = bottomStartX + 90 + 6;
            fill(poseStack, squareX, bottomY - 1, squareX + 10, bottomY + 9, currentCombinedColor);

            int resetX = squareX + 10 + 8;
            this.font.draw(poseStack, Component.translatable("orelocator.button.reset"),
                    resetX, bottomY, 0xAAAAAA);
        }

        this.font.draw(poseStack, Component.translatable("orelocator.gui.radius"),
                leftPos + 85, topPos + 132, 0xFFFFFF);
        radiusEdit.render(poseStack, mouseX, mouseY, partialTick);

        super.render(poseStack, mouseX, mouseY, partialTick);
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
        OreFilterHolder.setFilter(oreFilter);
        ModNetwork.CHANNEL.sendToServer(new SyncOreFilterPacket(oreFilter));
        super.onClose();
    }

    private static class OreEntry {
        final String key;
        final Component displayName;
        final ItemStack iconStack;

        OreEntry(String key, Component displayName, ItemStack iconStack) {
            this.key = key;
            this.displayName = displayName;
            this.iconStack = iconStack;
        }

        ItemStack getIconStack() {
            return iconStack;
        }
    }
}