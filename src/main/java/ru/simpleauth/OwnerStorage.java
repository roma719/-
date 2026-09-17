package ru.simpleauth;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Личное хранилище владельца (команда /ecc) — 54 слота, содержимое
 * переживает рестарт сервера.
 *
 * Сериализация сделана напрямую через ItemStack.OPTIONAL_CODEC (кодек,
 * который умеет и пустые стеки), а не через Inventories.writeNbt — в
 * 1.21.9+ эту утилиту переписали на ReadView/WriteView, а кодек предметов
 * стабильнее между версиями. Список всегда ровно из SIZE элементов по
 * порядку слотов (пустые тоже пишутся), поэтому индексы слотов хранить не
 * нужно — при загрузке читаем по порядку.
 */
public class OwnerStorage {

    private static final int SIZE = 54;
    private static SimpleInventory instance;
    private static RegistryWrapper.WrapperLookup registryLookup;

    private OwnerStorage() {
    }

    public static void init(MinecraftServer server) {
        registryLookup = server.getRegistryManager();
        if (instance == null) instance = new PersistentInventory(SIZE);
        load();
    }

    public static SimpleInventory get() {
        if (instance == null) instance = new PersistentInventory(SIZE);
        return instance;
    }

    private static Path file() {
        return ServerSettings.dir().resolve("owner_storage.dat");
    }

    @SuppressWarnings("unchecked")
    private static void load() {
        Path path = file();
        if (!Files.exists(path) || registryLookup == null) return;
        try {
            NbtCompound root = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
            NbtElement itemsNbt = root.get("Items");
            if (itemsNbt == null) return;

            RegistryOps<NbtElement> ops = registryLookup.getOps(NbtOps.INSTANCE);
            List<ItemStack> list = ItemStack.OPTIONAL_CODEC.listOf()
                    .parse(ops, itemsNbt).result().orElse(List.of());
            for (int i = 0; i < SIZE && i < list.size(); i++) {
                instance.setStack(i, list.get(i));
            }
        } catch (IOException e) {
            ServerCore.LOGGER.warn("Не удалось загрузить owner_storage.dat: {}", e.getMessage());
        }
    }

    private static void save() {
        if (instance == null || registryLookup == null) return;
        try {
            List<ItemStack> list = new ArrayList<>(SIZE);
            for (int i = 0; i < SIZE; i++) {
                list.add(instance.getStack(i));
            }
            RegistryOps<NbtElement> ops = registryLookup.getOps(NbtOps.INSTANCE);
            NbtElement encoded = ItemStack.OPTIONAL_CODEC.listOf()
                    .encodeStart(ops, list).getOrThrow();

            NbtCompound root = new NbtCompound();
            root.put("Items", encoded);
            Files.createDirectories(ServerSettings.dir());
            NbtIo.writeCompressed(root, file());
        } catch (Exception e) {
            ServerCore.LOGGER.warn("Не удалось сохранить owner_storage.dat: {}", e.getMessage());
        }
    }

    /** SimpleInventory, который сам сохраняется на диск при любом изменении. */
    private static class PersistentInventory extends SimpleInventory {
        PersistentInventory(int size) {
            super(size);
        }

        @Override
        public void markDirty() {
            super.markDirty();
            OwnerStorage.save();
        }
    }
}
