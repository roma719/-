package ru.simpleauth;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;

import java.util.function.Predicate;

/**
 * Категории каталога /cursedshop. Порядок важен — предмет попадает в
 * первую категорию, под которую подходит (проверяются по порядку сверху
 * вниз). БЛОКИ и РАЗНОЕ — catch-all в самом конце, иначе бы забрали себе
 * всё подряд раньше более узких категорий.
 *
 * В 1.21.9+ Mojang убрал классы ArmorItem/SwordItem/ToolItem — теперь тип
 * предмета определяется по компонентам данных, а не по классу. Поэтому
 * броня = есть компонент EQUIPPABLE, оружие/инструменты = ломается
 * (MAX_DAMAGE) и при этом не надевается.
 *
 * Категория КНИГИ обрабатывается особо — помимо самих книг из этого
 * фильтра, в неё дополнительно подмешиваются все зачарованные книги на
 * все уровни (см. EnchantedBooksCatalog).
 */
public enum ShopCategory {

    BOOKS("📚 Книги и зачарования", Items.ENCHANTED_BOOK, item ->
            item == Items.BOOK || item == Items.WRITABLE_BOOK || item == Items.WRITTEN_BOOK
                    || item == Items.KNOWLEDGE_BOOK),

    POTIONS("🧪 Зелья", Items.POTION, item ->
            item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION
                    || item == Items.EXPERIENCE_BOTTLE),

    FOOD("🍖 Еда", Items.COOKED_BEEF, item ->
            new ItemStack(item).contains(DataComponentTypes.FOOD)),

    ARMOR("🛡 Броня", Items.DIAMOND_CHESTPLATE, item ->
            new ItemStack(item).contains(DataComponentTypes.EQUIPPABLE)),

    WEAPONS_TOOLS("⚔ Оружие и инструменты", Items.DIAMOND_SWORD, item -> {
        ItemStack stack = new ItemStack(item);
        return stack.contains(DataComponentTypes.MAX_DAMAGE)
                && !stack.contains(DataComponentTypes.EQUIPPABLE);
    }),

    SPAWN_EGGS("🥚 Яйца призыва", Items.PIG_SPAWN_EGG, item -> item instanceof SpawnEggItem),

    BLOCKS("🧱 Блоки", Items.BRICKS, item -> item instanceof BlockItem),

    MISC("📦 Разное", Items.CHEST, item -> true); // catch-all, обязательно последним

    public final String label;
    public final Item icon;
    public final Predicate<Item> matcher;

    ShopCategory(String label, Item icon, Predicate<Item> matcher) {
        this.label = label;
        this.icon = icon;
        this.matcher = matcher;
    }

    /** Первая подходящая категория для предмета, по порядку объявления. */
    public static ShopCategory of(Item item) {
        for (ShopCategory category : values()) {
            if (category.matcher.test(item)) return category;
        }
        return MISC;
    }
}
