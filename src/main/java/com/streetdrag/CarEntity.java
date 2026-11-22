package com.streetdrag;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Гоночная тачка. Управление считает клиент водителя (как у лодки), сервер принимает позицию.
 * W/S — газ/тормоз, A/D — руль, Shift — выйти. Нитро (эффект скорости) даёт ускорение.
 * Тюнинг: ПКМ по машине двигателем, турбиной, шиной или красителем.
 */
public class CarEntity extends Entity {
	public static final String[] COLORS = {"red", "blue", "yellow", "green", "black", "white", "orange", "purple"};
	public static final int MAX_ENGINE = 3;
	public static final int MAX_TURBO = 2;
	public static final int MAX_TIRES = 3;

	private static final double BASE_MAX_SPEED = 0.95; // блоков за тик (~19 б/с)
	private static final double BASE_ACCEL = 0.022;
	private static final double HARD_CAP = 1.8;

	private static final EntityDataAccessor<Integer> DATA_ENGINE = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_TURBO = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_TIRES = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);

	private double speed;

	public CarEntity(EntityType<? extends CarEntity> type, Level level) {
		super(type, level);
	}

	// ---------- данные / характеристики ----------

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ENGINE, 0);
		builder.define(DATA_TURBO, 0);
		builder.define(DATA_TIRES, 0);
		builder.define(DATA_COLOR, 0);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		setEngine(input.getIntOr("Engine", 0));
		setTurbo(input.getIntOr("Turbo", 0));
		setTires(input.getIntOr("Tires", 0));
		setColor(input.getIntOr("Color", 0));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("Engine", getEngine());
		output.putInt("Turbo", getTurbo());
		output.putInt("Tires", getTires());
		output.putInt("Color", getColor());
	}

	public int getEngine() {
		return this.entityData.get(DATA_ENGINE);
	}

	public int getTurbo() {
		return this.entityData.get(DATA_TURBO);
	}

	public int getTires() {
		return this.entityData.get(DATA_TIRES);
	}

	public int getColor() {
		return this.entityData.get(DATA_COLOR);
	}

	public void setEngine(int v) {
		this.entityData.set(DATA_ENGINE, Math.max(0, Math.min(MAX_ENGINE, v)));
	}

	public void setTurbo(int v) {
		this.entityData.set(DATA_TURBO, Math.max(0, Math.min(MAX_TURBO, v)));
	}

	public void setTires(int v) {
		this.entityData.set(DATA_TIRES, Math.max(0, Math.min(MAX_TIRES, v)));
	}

	public void setColor(int v) {
		this.entityData.set(DATA_COLOR, Math.floorMod(v, COLORS.length));
	}

	/** Максимальная скорость без нитро, блоков за тик. */
	public double maxSpeed() {
		double m = BASE_MAX_SPEED * (1.0 + 0.12 * getEngine()) * (1.0 + 0.10 * getTurbo());
		return Math.min(m, HARD_CAP);
	}

	public double acceleration() {
		return BASE_ACCEL * (1.0 + 0.15 * getEngine()) * (1.0 + 0.10 * getTurbo());
	}

	public double grip() {
		return 1.0 + 0.12 * getTires();
	}

	/** Набор для авто с сохранённым тюнингом. */
	public ItemStack toKit() {
		ItemStack kit = new ItemStack(ModItems.CAR_KIT);
		CompoundTag tag = new CompoundTag();
		tag.putInt("engine", getEngine());
		tag.putInt("turbo", getTurbo());
		tag.putInt("tires", getTires());
		tag.putInt("color", getColor());
		kit.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return kit;
	}

	public void applyKit(ItemStack kit) {
		CustomData data = kit.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return;
		}
		CompoundTag tag = data.copyTag();
		setEngine(tag.getIntOr("engine", 0));
		setTurbo(tag.getIntOr("turbo", 0));
		setTires(tag.getIntOr("tires", 0));
		setColor(tag.getIntOr("color", 0));
	}

	// ---------- взаимодействие ----------

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public float maxUpStep() {
		return 1.0F; // заезжает на полные блоки
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().isEmpty();
	}

	@Override
	public LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof Player player ? player : null;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!(this.level() instanceof ServerLevel)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			if (held.isEmpty()) {
				showInfo(player);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}
		if (tryTune(player, held)) {
			return InteractionResult.SUCCESS;
		}
		player.startRiding(this);
		return InteractionResult.SUCCESS;
	}

	private boolean tryTune(Player player, ItemStack held) {
		if (held.isEmpty()) {
			return false;
		}
		if (held.is(ModItems.ENGINE)) {
			return upgrade(player, held, getEngine(), MAX_ENGINE, "Двигатель", this::setEngine);
		}
		if (held.is(ModItems.TURBO)) {
			return upgrade(player, held, getTurbo(), MAX_TURBO, "Турбина", this::setTurbo);
		}
		if (held.is(ModItems.TIRE)) {
			return upgrade(player, held, getTires(), MAX_TIRES, "Шины", this::setTires);
		}
		int color = dyeToColor(held);
		if (color >= 0) {
			if (getColor() == color) {
				return true;
			}
			setColor(color);
			held.consume(1, player);
			this.playSound(SoundEvents.DYE_USE, 1.0F, 1.0F);
			player.displayClientMessage(Component.literal("Машина перекрашена."), true);
			return true;
		}
		return false;
	}

	private boolean upgrade(Player player, ItemStack held, int current, int max, String name, java.util.function.IntConsumer setter) {
		if (current >= max) {
			player.displayClientMessage(Component.literal(name + ": уже максимум (" + max + "/" + max + ")."), true);
			return true;
		}
		setter.accept(current + 1);
		held.consume(1, player);
		this.playSound(SoundEvents.ANVIL_USE, 0.8F, 1.2F);
		player.displayClientMessage(Component.literal(name + ": " + (current + 1) + "/" + max
			+ ". Макс. скорость ~" + Math.round(maxSpeed() * 20) + " бл/с"), true);
		return true;
	}

	private static int dyeToColor(ItemStack s) {
		if (s.is(Items.RED_DYE)) return 0;
		if (s.is(Items.BLUE_DYE)) return 1;
		if (s.is(Items.YELLOW_DYE)) return 2;
		if (s.is(Items.GREEN_DYE)) return 3;
		if (s.is(Items.BLACK_DYE)) return 4;
		if (s.is(Items.WHITE_DYE)) return 5;
		if (s.is(Items.ORANGE_DYE)) return 6;
		if (s.is(Items.PURPLE_DYE)) return 7;
		return -1;
	}

	private void showInfo(Player player) {
		player.displayClientMessage(Component.literal("Двигатель " + getEngine() + "/" + MAX_ENGINE
			+ "   Турбина " + getTurbo() + "/" + MAX_TURBO
			+ "   Шины " + getTires() + "/" + MAX_TIRES
			+ "   Макс. скорость ~" + Math.round(maxSpeed() * 20) + " бл/с"), false);
	}

	/** Shift + удар по пустой машине — забрать её обратно в набор (тюнинг сохраняется). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && player.isShiftKeyDown() && this.getPassengers().isEmpty()) {
			this.spawnAtLocation(level, toKit());
			this.discard();
			return true;
		}
		return false;
	}

	// ---------- физика ----------

	@Override
	public void tick() {
		super.tick();
		if (this.isLocalInstanceAuthoritative()) {
			LivingEntity driver = this.getControllingPassenger();
			float forward = 0.0F;
			float strafe = 0.0F;
			boolean nitro = false;
			if (driver != null) {
				forward = driver.zza;
				strafe = driver.xxa;
				nitro = driver.hasEffect(MobEffects.SPEED);
			}

			double max = Math.min(maxSpeed() * (nitro ? 1.5 : 1.0), HARD_CAP * 1.5);
			double accel = acceleration() * (nitro ? 2.0 : 1.0);
			if (forward > 0) {
				double frac = Math.max(0.0, this.speed) / max;
				this.speed += accel * Math.max(0.15, 1.25 - frac); // на высокой передаче разгон слабее
			} else if (forward < 0) {
				this.speed -= BASE_ACCEL * 1.6;
			} else {
				this.speed *= 0.97;
				if (Math.abs(this.speed) < 0.004) {
					this.speed = 0.0;
				}
			}
			this.speed = Math.max(-max * 0.35, Math.min(max, this.speed));

			if (strafe != 0 && Math.abs(this.speed) > 0.02) {
				double sf = Math.max(0.35, Math.min(1.0, Math.abs(this.speed) / 0.45));
				float turn = (float) (strafe * 4.0 * sf * grip() * Math.signum(this.speed));
				this.setYRot(this.getYRot() - turn);
			}
			this.setXRot(0.0F);

			double rad = Math.toRadians(this.getYRot());
			Vec3 old = this.getDeltaMovement();
			double dy = this.onGround() ? -0.1 : (old.y - 0.08) * 0.98;
			this.setDeltaMovement(-Math.sin(rad) * this.speed, dy, Math.cos(rad) * this.speed);
			this.move(MoverType.SELF, this.getDeltaMovement());
			if (this.horizontalCollision) {
				this.speed *= 0.4;
			}

			// огонь из выхлопа при нитро, дымок на разгоне
			if (this.speed > 0.2) {
				double bx = this.getX() + Math.sin(rad) * 1.0;
				double bz = this.getZ() - Math.cos(rad) * 1.0;
				if (nitro) {
					this.level().addParticle(ParticleTypes.FLAME, bx, this.getY() + 0.4, bz, 0.0, 0.0, 0.0);
				} else if (forward > 0 && this.random.nextInt(3) == 0) {
					this.level().addParticle(ParticleTypes.SMOKE, bx, this.getY() + 0.3, bz, 0.0, 0.0, 0.0);
				}
			}
		} else {
			this.setDeltaMovement(Vec3.ZERO);
		}
	}
}
