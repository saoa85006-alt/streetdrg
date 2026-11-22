package com.streetdrag;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 4 входных слота (0-3) + 1 выходной (4). Один тип предмета занимает максимум один входной слот,
 * чтобы воронка не забила все слоты чем-то одним и не заблокировала линию.
 */
public class MachineBlockEntity extends BlockEntity implements WorldlyContainer {
	public static final int INPUTS = 4;
	public static final int OUTPUT = 4;

	private NonNullList<ItemStack> items = NonNullList.withSize(INPUTS + 1, ItemStack.EMPTY);
	private int progress;

	public MachineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MACHINE, pos, state);
	}

	// ---------- логика ----------

	public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
		if (!(state.getBlock() instanceof MachineBlock block)) {
			return;
		}
		MachineType.Recipe recipe = be.findRecipe(block.type);
		if (recipe == null || !be.canOutput(recipe)) {
			if (be.progress != 0) {
				be.progress = 0;
			}
			return;
		}
		be.progress++;
		if (be.progress >= recipe.ticks()) {
			be.progress = 0;
			be.consume(recipe);
			ItemStack out = be.items.get(OUTPUT);
			if (out.isEmpty()) {
				be.items.set(OUTPUT, new ItemStack(recipe.output().get(), recipe.outCount()));
			} else {
				out.grow(recipe.outCount());
			}
			be.setChanged();
		}
	}

	private MachineType.Recipe findRecipe(MachineType type) {
		for (MachineType.Recipe r : type.recipes) {
			boolean ok = true;
			for (MachineType.Ing ing : r.inputs()) {
				if (count(ing) < ing.count()) {
					ok = false;
					break;
				}
			}
			if (ok) {
				return r;
			}
		}
		return null;
	}

	private int count(MachineType.Ing ing) {
		int total = 0;
		for (int i = 0; i < INPUTS; i++) {
			ItemStack s = items.get(i);
			if (s.is(ing.item().get())) {
				total += s.getCount();
			}
		}
		return total;
	}

	private boolean canOutput(MachineType.Recipe r) {
		ItemStack out = items.get(OUTPUT);
		if (out.isEmpty()) {
			return true;
		}
		return out.is(r.output().get()) && out.getCount() + r.outCount() <= out.getMaxStackSize();
	}

	private void consume(MachineType.Recipe r) {
		for (MachineType.Ing ing : r.inputs()) {
			int left = ing.count();
			for (int i = 0; i < INPUTS && left > 0; i++) {
				ItemStack s = items.get(i);
				if (s.is(ing.item().get())) {
					int take = Math.min(left, s.getCount());
					s.shrink(take);
					left -= take;
				}
			}
		}
	}

	/** Кладёт предмет во входной слот. Возвращает, сколько штук реально перенесено. */
	public int insertStack(ItemStack stack) {
		for (int i = 0; i < INPUTS; i++) {
			if (!canPlaceItem(i, stack)) {
				continue;
			}
			ItemStack cur = items.get(i);
			if (cur.isEmpty()) {
				int n = Math.min(stack.getCount(), stack.getMaxStackSize());
				items.set(i, stack.copyWithCount(n));
				setChanged();
				return n;
			}
			if (!ItemStack.isSameItemSameComponents(cur, stack)) {
				continue; // нельзя клеить разные предметы в один стак
			}
			int space = cur.getMaxStackSize() - cur.getCount();
			if (space > 0) {
				int n = Math.min(space, stack.getCount());
				cur.grow(n);
				setChanged();
				return n;
			}
		}
		return 0;
	}

	public ItemStack takeOutput() {
		ItemStack out = items.get(OUTPUT);
		if (out.isEmpty()) {
			return ItemStack.EMPTY;
		}
		items.set(OUTPUT, ItemStack.EMPTY);
		setChanged();
		return out;
	}

	public List<Component> describe(MachineType type) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("block.streetdrag." + type.id).withStyle(ChatFormatting.GOLD));
		for (MachineType.Recipe r : type.recipes) {
			StringBuilder sb = new StringBuilder();
			if (r.inputs().isEmpty()) {
				sb.append("Добыча без сырья");
			}
			for (MachineType.Ing ing : r.inputs()) {
				if (sb.length() > 0) {
					sb.append(" + ");
				}
				sb.append(ing.count()).append("x ").append(new ItemStack(ing.item().get()).getHoverName().getString());
			}
			sb.append(" -> ").append(r.outCount()).append("x ")
				.append(new ItemStack(r.output().get()).getHoverName().getString())
				.append(" (").append(r.ticks() / 20.0).append(" c)");
			lines.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
		}
		StringBuilder inside = new StringBuilder();
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack s = items.get(i);
			if (!s.isEmpty()) {
				if (inside.length() > 0) {
					inside.append(", ");
				}
				inside.append(i == OUTPUT ? "[выход] " : "").append(s.getCount()).append("x ").append(s.getHoverName().getString());
			}
		}
		lines.add(Component.literal("Внутри: " + (inside.length() == 0 ? "пусто" : inside)).withStyle(ChatFormatting.AQUA));
		lines.add(Component.literal("ПКМ предметом — загрузить, Shift+ПКМ — забрать результат. Воронки сверху/сбоку — вход, снизу — выход.")
			.withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	// ---------- сохранение ----------

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(INPUTS + 1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		progress = input.getIntOr("Progress", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("Progress", progress);
	}

	// выпасть содержимое при ломании блока
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropContents(level, pos, this);
		}
	}

	// ---------- Container / WorldlyContainer ----------

	@Override
	public int getContainerSize() {
		return INPUTS + 1;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack s : items) {
			if (!s.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack r = ContainerHelper.removeItem(items, slot, amount);
		if (!r.isEmpty()) {
			setChanged();
		}
		return r;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot >= INPUTS || !(getBlockState().getBlock() instanceof MachineBlock block) || !block.type.accepts(stack)) {
			return false;
		}
		ItemStack here = items.get(slot);
		if (!here.isEmpty() && !ItemStack.isSameItemSameComponents(here, stack)) {
			return false;
		}
		for (int j = 0; j < INPUTS; j++) {
			if (j != slot && ItemStack.isSameItem(items.get(j), stack)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[] {0, 1, 2, 3, 4};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
		return slot == OUTPUT;
	}
}
