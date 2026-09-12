package mypals.ml;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Trajectory implements ModInitializer {
	public static Set<Item> Type1 = new HashSet<>();
	public static Set<Item> Type2 = new HashSet<>();
	public static final String MOD_ID = "trajectory";

	public static boolean showInfo = false;
	public static int handSide = 1;

	public static Vec3d hitPoint = new Vec3d(0, 0, 0);

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
			if(showInfo)
				DrawableHelper.drawTextWithShadow(matrixStack, MinecraftClient.getInstance().textRenderer, "Predicted landing site: x:" + String.format("%.1f", hitPoint.x).formatted(Formatting.RED) + " y:"+ String.format("%.1f", hitPoint.y).formatted(Formatting.GREEN) +" z:"+String.format("%.1f", hitPoint.z).formatted(Formatting.BLUE), 2, 2, 0xFFFFFF);
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world != null && client.player != null) {
				onClientTick(client);
			}
		});

		LOGGER.info("Hello Fabric world!");

		Type1.add(Items.ENDER_PEARL.asItem());
		Type1.add(Items.SNOWBALL.asItem());
		Type1.add(Items.EGG.asItem());

		Type2.add(Items.BOW.asItem());
		Type2.add(Items.TRIDENT.asItem());
		Type2.add(Items.CROSSBOW.asItem());
	}
	private static boolean isNormalProjectile(ItemStack stack) {
		return Type1.contains(stack.getItem());
	}

	private static boolean isComplexProjectile(ItemStack stack) {
		return Type2.contains(stack.getItem());
	}

	private void onClientTick(MinecraftClient client) {
		World world = client.world;
		ForThisPlayer();
		ForOtherPlayers();
		ForOtherEntitys();
        assert world != null;
        ForProjectiles(ParticleTypes.FLAME, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.SONIC_BOOM,world, 128);
	}
	public static void ForThisPlayer()
	{
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;
		World world = client.world;
        assert player != null;
        float pitch = player.getPitch();
		float yaw = player.getYaw();
		ItemStack itemStack = player.getMainHandStack();
		ItemStack itemStackAlt = player.getOffHandStack();
		if (player != null) {
			if (isNormalProjectile(itemStack) || isComplexProjectile(itemStack)) { handSide = 1;showInfo = true;}
			else if (isNormalProjectile(itemStackAlt) || isComplexProjectile(itemStackAlt)) { handSide = -1; showInfo = true;}
			else {showInfo = false;}

			if (isNormalProjectile(itemStack) || isNormalProjectile(itemStackAlt)) {

				float speed = 1.5f;
				float gravity = 0.03f;

				// 执行简单投掷物轨迹计算
				renderTrajectoryWithParticles(ParticleTypes.GLOW, ParticleTypes.SONIC_BOOM,world, player, pitch,yaw, speed, gravity, true);
			} else if (isComplexProjectile(itemStack) || isComplexProjectile(itemStackAlt)) {
				// 执行复杂投掷物轨迹计算

				float bowMultiplier = (72000.0f - player.getItemUseTimeLeft()) / 20.0f;
				bowMultiplier = (bowMultiplier * bowMultiplier + bowMultiplier * 2.0f) / 3.0f;
				if (bowMultiplier > 1.0f) {
					bowMultiplier = 1.0f;
				}
				float gravity = 0.05f;
				float speed = bowMultiplier * 3.0f;
				if(player.getItemUseTimeLeft() == 0)
				{
					speed = 0f;
				}
				renderTrajectoryWithParticles(ParticleTypes.GLOW, ParticleTypes.SONIC_BOOM,world, player, pitch,yaw, speed, gravity, true);
			}
		}
	}
	public static void ForOtherPlayers()
	{
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity currentPlayer = client.player;
		World world = client.world;
		if ( client.world != null) {
			// 获取当前世界中的所有玩家
			List<AbstractClientPlayerEntity> players =  client.world.getPlayers();

			// 遍历玩家列表，并排除当前玩家
			for (PlayerEntity p : players) {
				if (p != currentPlayer) {
					if (p != null) {
						float pitch = p.getPitch();
						float yaw = p.getYaw();
						ItemStack itemStack = p.getMainHandStack();
						ItemStack itemStackAlt = p.getOffHandStack();
						if (isNormalProjectile(itemStack) || isNormalProjectile(itemStack)) { handSide = 1;}
						else if (isNormalProjectile(itemStackAlt) || isNormalProjectile(itemStackAlt)) { handSide = -1; };

						if (isNormalProjectile(itemStack) || isNormalProjectile(itemStackAlt)) {

							float speed = 1.5f;
							float gravity = 0.03f;

							// 执行简单投掷物轨迹计算
							renderTrajectoryWithParticles(ParticleTypes.ELECTRIC_SPARK, ParticleTypes.SONIC_BOOM, world, p, pitch,yaw, speed, gravity, false);
						} else if (isComplexProjectile(itemStack) || isComplexProjectile(itemStackAlt)) {
							// 执行复杂投掷物轨迹计算

							float bowMultiplier = (72000.0f - p.getItemUseTimeLeft()) / 20.0f;
							bowMultiplier = (bowMultiplier * bowMultiplier + bowMultiplier * 2.0f) / 3.0f;
							if (bowMultiplier > 1.0f) {
								bowMultiplier = 1.0f;
							}
							float gravity = 0.05f;
							float speed = bowMultiplier * 3.0f;
							if(p.getItemUseTimeLeft() == 0)
							{
								speed = 0f;
							}
							renderTrajectoryWithParticles(ParticleTypes.ELECTRIC_SPARK, ParticleTypes.SONIC_BOOM, world, p, pitch,yaw, speed, gravity, false);
						}
					}
				}
			}
		}
	}
	public static void ForOtherEntitys()
	{
		MinecraftClient client = MinecraftClient.getInstance();
		World world = client.world;

		if (client.world != null) {
			// 遍历当前世界中的所有实体
			for (Entity entity : client.world.getEntities()) {
				// 确保该实体不是玩家
				if (!(entity instanceof PlayerEntity)) {
					// 检查该实体是否是活着的实体并且是生物
					if (entity instanceof LivingEntity livingEntity) {
                        float pitch = livingEntity.getPitch();
						float yaw = livingEntity.getYaw();
						ItemStack itemStack = livingEntity.getMainHandStack();
						ItemStack itemStackAlt = livingEntity.getOffHandStack();
						if (isNormalProjectile(itemStack) || isNormalProjectile(itemStack)) { handSide = 1;}
						else if (isNormalProjectile(itemStackAlt) || isNormalProjectile(itemStackAlt)) { handSide = -1; };

						if (isNormalProjectile(itemStack) || isNormalProjectile(itemStackAlt)) {

							float speed = 1.5f;
							float gravity = 0.03f;

							renderTrajectoryWithParticles(ParticleTypes.WAX_ON, ParticleTypes.SONIC_BOOM,world, livingEntity, pitch,yaw, speed, gravity, false);
						} else if (isComplexProjectile(itemStack) || isComplexProjectile(itemStackAlt)) {
							float bowMultiplier = (72000.0f - livingEntity.getItemUseTimeLeft()) / 20.0f;
							bowMultiplier = (bowMultiplier * bowMultiplier + bowMultiplier * 2.0f) / 3.0f;
							if (bowMultiplier > 1.0f) {
								bowMultiplier = 1.0f;
							}
							float gravity = 0.05f;
							float speed = bowMultiplier * 3.0f;
							if(livingEntity.getItemUseTimeLeft() == 0)
							{
								speed = 0f;
							}
							renderTrajectoryWithParticles(ParticleTypes.WAX_ON, ParticleTypes.SONIC_BOOM, world, livingEntity, pitch,yaw, speed, gravity, false);
						}
					}
				}
			}
		}
	}
	public static List<Vec3d> calculateTrajectory( ParticleEffect particleHit, World world, float pitch, float yaw, LivingEntity player, float speed, float gravity) {
		if(speed > 0) {
			List<Vec3d> trajectoryPoints = new ArrayList<>();
			double posX = player.getX();
			double posY = player.getY();
			double posZ = player.getZ();

			float drag = 0.99f;

			float VX = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
			float VY = -MathHelper.sin(pitch * 0.017453292F);
			float VZ = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
			Vec3d speedVecC = (new Vec3d(VX, VY, VZ)).normalize().multiply(speed);
			Vec3d Velocity = new Vec3d(speedVecC.x, speedVecC.y, speedVecC.z);
			Vec3d entityPos = new Vec3d(0, 0 + 1.5, 0);

			SnowballEntity modleEntity = new SnowballEntity(world, player);

			for (int i = 0; i < 100; i++) {
				HitResult hitResult = world.raycast(new RaycastContext(new Vec3d(posX + entityPos.x, posY + entityPos.y, posZ + entityPos.z), new Vec3d(posX + entityPos.x, posY + entityPos.y, posZ + entityPos.z).add(Velocity), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, modleEntity));
				if (hitResult.getType() != HitResult.Type.MISS) {
					hitPoint = new Vec3d(hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z);
					world.addParticle(particleHit, hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z, 0, 0, 0);
					break;
				}


				entityPos = entityPos.add(Velocity);
				Velocity = Velocity.multiply(drag);
				Velocity = new Vec3d(Velocity.x, Velocity.y - gravity, Velocity.z);

				Vec3d startPos = new Vec3d(posX + entityPos.x, posY + entityPos.y, posZ + entityPos.z);
				trajectoryPoints.add(new Vec3d(startPos.x, startPos.y, startPos.z));
			}
			return trajectoryPoints;
		}
		else
			return new ArrayList<>();
	}

	public List<Vec3d> calculateTrajectoryForEntity(ParticleEffect particleHit,Entity entity, World world, double gravity, float drag) {
		List<Vec3d> trajectoryPoints = new ArrayList<>();
		double posX = entity.getX();
		double posY = entity.getY();
		double posZ = entity.getZ();
		Vec3d velocity = entity.getVelocity();
		for (int i = 0; i < 100; i++) {
			Vec3d newPosition = new Vec3d(posX, posY, posZ).add(velocity);
			HitResult hitResult = world.raycast(new RaycastContext(
					new Vec3d(posX, posY, posZ),
					newPosition,
					RaycastContext.ShapeType.OUTLINE,
					RaycastContext.FluidHandling.NONE,
					entity));

			if (hitResult.getType() != HitResult.Type.MISS) {
				world.addParticle(particleHit, hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z, 0, 0, 0);
				break;
			}
			trajectoryPoints.add(newPosition);
			posX = newPosition.x;
			posY = newPosition.y;
			posZ = newPosition.z;
			velocity = velocity.multiply(drag);
			velocity = new Vec3d(velocity.x, velocity.y - gravity, velocity.z);
		}

		return trajectoryPoints;
	}



	public static List<Vec3d> applyRightOffset(LivingEntity owner,List<Vec3d> points, double maxOffset) {
		int n = points.size() - 1; // 点的总数减1

		// 创建一个新的List来存储偏移后的点
		List<Vec3d> offsetPoints = new ArrayList<>();

		double yaw = Math.toRadians(owner.getYaw());

		// 计算玩家本地坐标系的Z轴正方向（前进方向）
		double forwardDirectionX = -Math.cos(yaw);
		double forwardDirectionZ = -Math.sin(yaw);

		for (int i = 0; i <= n; i++) {
			Vec3d point = points.get(i);

			// 计算偏移量
			double offset = maxOffset * (1 - (double)i / n);

			// 偏移点的位置
			Vec3d offsetPoint = new Vec3d(
					point.x + offset * forwardDirectionX,
					point.y,
					point.z + offset * forwardDirectionZ
			);

			// 添加到偏移点列表
			offsetPoints.add(offsetPoint);
		}

		return offsetPoints;
	}

	public void ForProjectiles(ParticleEffect mark, ParticleEffect particleTrace, ParticleEffect particleHit, World world, float boxSize) {
		PlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        Box searchBox = new Box(player.getX()-boxSize, player.getY()-boxSize, player.getZ()-boxSize, player.getX()+boxSize, player.getY()+boxSize, player.getZ()+boxSize);
		List<Entity> projectilesSimple = new ArrayList<>();
		List<Entity> projectilesComplex= new ArrayList<>();
		List<Entity> projectilesStrate= new ArrayList<>();

		projectilesSimple.addAll(world.getEntitiesByType(EntityType.SNOWBALL,searchBox, entity -> true));
		projectilesSimple.addAll(world.getEntitiesByType(EntityType.EGG,searchBox, entity -> true));
		projectilesSimple.addAll(world.getEntitiesByType(EntityType.POTION,searchBox, entity -> true));
		projectilesSimple.addAll(world.getEntitiesByType(EntityType.ENDER_PEARL,searchBox, entity -> true));

		projectilesComplex.addAll(world.getEntitiesByType(EntityType.TRIDENT, searchBox, trident -> !trident.isOnGround()));
		projectilesComplex.addAll(world.getEntitiesByType(EntityType.SPECTRAL_ARROW,searchBox, spectralArrow -> !spectralArrow.isOnGround()));
		projectilesComplex.addAll(world.getEntitiesByType(EntityType.ARROW,searchBox, arrow -> !arrow.isOnGround()));

		projectilesStrate.addAll(world.getEntitiesByType(EntityType.FIREBALL, searchBox, entity -> true));
		projectilesStrate.addAll(world.getEntitiesByType(EntityType.DRAGON_FIREBALL, searchBox, entity -> true));
		projectilesStrate.addAll(world.getEntitiesByType(EntityType.SMALL_FIREBALL, searchBox, entity -> true));
		projectilesStrate.addAll(world.getEntitiesByType(EntityType.WITHER_SKULL, searchBox, entity -> true));
		projectilesStrate.addAll(world.getEntitiesByType(EntityType.SHULKER_BULLET, searchBox, entity -> true));

		for (Entity projectile : projectilesSimple) {
			List<Vec3d> points = calculateTrajectoryForEntity(particleHit, projectile, world, 0.03f, 0.99f);
			world.addParticle(mark, projectile.getPos().x, projectile.getPos().y, projectile.getPos().z, 0, 0, 0);
			for(Vec3d v: points)
			{
				world.addParticle(particleTrace, v.x, v.y, v.z,0, 0, 0);

			}
		}
		for (Entity projectile : projectilesComplex) {
			List<Vec3d> points = calculateTrajectoryForEntity(particleHit, projectile, world,0.05f, 0.99f);
			world.addParticle(mark, projectile.getPos().x, projectile.getPos().y, projectile.getPos().z, 0, 0, 0);
			for(Vec3d v: points)
			{
				world.addParticle(particleTrace, v.x, v.y, v.z,0, 0, 0);

			}
		}
		for (Entity projectile : projectilesStrate) {
			List<Vec3d> points = calculateTrajectoryForEntity(particleHit, projectile, world,0f, 0.99f);
			world.addParticle(mark, projectile.getPos().x, projectile.getPos().y, projectile.getPos().z, 0, 0, 0);
			for(Vec3d v: points)
			{
				world.addParticle(particleTrace, v.x, v.y, v.z,0, 0, 0);

			}
		}
	}

	public static void renderTrajectoryWithParticles(ParticleEffect particleTrace, ParticleEffect particleHit, World world, LivingEntity owner, float pitch, float yaw, float speed, float gravity, boolean doOffset) {
		List<Vec3d> points = calculateTrajectory(particleHit ,world, pitch, yaw, owner, speed, gravity);
		if(doOffset) {
			if (handSide == 1)
				points = applyRightOffset(owner, points, 1f);
			else
				points = applyRightOffset(owner, points, -1f);
		}
		for(Vec3d v: points)
		{
			world.addParticle(particleTrace, v.x, v.y, v.z,0, 0, 0);
		}
	}

}