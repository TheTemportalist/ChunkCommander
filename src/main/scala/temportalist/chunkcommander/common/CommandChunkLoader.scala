package temportalist.chunkcommander.common

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.mojang.authlib.GameProfile
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.world.{ChunkCoordIntPair, World}
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.ForgeChunkManager.Ticket
import temportalist.chunkcommander.api.ChunkLoader
import temportalist.chunkcommander.common.world.{DimensionChunkPair, WorldDataChunks, WorldDataHandler}
import temportalist.origin.api.common.utility.{Players, WorldHelper}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by TheTemportalist on 1/14/2016.
  */
object CommandChunkLoader extends ChunkLoader {

	override def getMod: AnyRef = ChunkCommander

	private val temporaryByUUID = mutable.Map[UUID, ListBuffer[DimensionChunkPair]]()
	private val temporaryToStartTime = mutable.Map[UUID, Long]()

	private def constructForTemp(uuid: UUID,
			world: World, chunk: ChunkCoordIntPair): DimensionChunkPair = {
		val chunkPair = new DimensionChunkPair(world, chunk)
		if (!this.temporaryByUUID.contains(uuid))
			this.temporaryByUUID(uuid) = ListBuffer[DimensionChunkPair]()
		chunkPair
	}

	private def getPlayers(playerNames: String*): Array[UUID] = {
		val uuidList = ListBuffer[UUID]()
		for (name <- playerNames) {
			val id = this.getPlayerUUID(name)
			if (id != null) uuidList += id
		}
		uuidList.toArray
	}

	private def getPlayerUUID(name: String): UUID = {
		val profile =
			MinecraftServer.getServer.getPlayerProfileCache.getGameProfileForUsername(name)
		if (profile != null) profile.getId
		else null
	}

	private def getUUIDFromAmbiguous(playerNameOrID: AnyRef): UUID = {
		playerNameOrID match {
			case name: String => this.getPlayerUUID(name)
			case id: UUID => id
			case profile: GameProfile => profile.getId
			case _ => null
		}
	}

	private def forWorld(world: World): WorldDataChunks = {
		WorldDataHandler.forWorld[WorldDataChunks](world)
	}

	private def forWorld(dim: Int): WorldDataChunks = this.forWorld(WorldHelper.getWorld(dim))

	private def getWorld(world: World, dim: Int): World = {
		if (world.provider.getDimensionId == dim) world
		else WorldHelper.getWorld(dim)
	}

	def forceWithPlayers(worldIn: World, chunk: ChunkCoordIntPair, dim: Int,
			playerNames: String*): Unit = {
		val world = this.getWorld(worldIn, dim)
		val worldData = this.forWorld(world)
		if (!(worldData contains chunk) && this.forceLoadChunk(world, chunk)) {
			worldData.addChunk(chunk, this.getPlayers(playerNames: _*), System.currentTimeMillis())
		}
	}

	def load(worldIn: World, chunk: ChunkCoordIntPair, dim: Int, playerNameOrID: AnyRef): Unit = {
		val uuid = this.getUUIDFromAmbiguous(playerNameOrID)
		val world = this.getWorld(worldIn, dim)
		val chunkPair = this.constructForTemp(uuid, world, chunk)
		if (this.forWorld(world).contains(chunk)) return
		val name = Players.getUserName(uuid)
		if (this.forceLoadChunkPlayer(name, uuid, world, chunkPair)) {
			this.temporaryByUUID(uuid) += chunkPair
			this.temporaryToStartTime(uuid) = 0L
		}
		this.forWorld(world).updateDimension()
	}

	def unload(world: World, chunk: ChunkCoordIntPair, playerNameOrID: AnyRef): Unit = {
		this.unload(new DimensionChunkPair(world, chunk), this.getUUIDFromAmbiguous(playerNameOrID))
	}

	def unload(chunkPair: DimensionChunkPair, uuid: UUID): Unit = {
		if (this.unforceChunkPlayer(uuid, chunkPair)) {
			if (this.temporaryByUUID(uuid).contains(chunkPair))
				this.temporaryByUUID(uuid) -= chunkPair
			if (this.temporaryByUUID(uuid).isEmpty) {
				this.temporaryByUUID remove uuid
				if (this.temporaryToStartTime.contains(uuid))
					this.temporaryToStartTime remove uuid
			}
			this.forWorld(chunkPair.getDimension).updateDimension()
		}
	}

	def unload(uuid: UUID): Unit = {
		for (chunk <- this.temporaryByUUID(uuid)) this.unload(chunk, uuid)
	}

	def addPlayer(worldIn: World, chunk: ChunkCoordIntPair, dim: Int,
			playerNameOrID: AnyRef): Unit = {
		val world = this.getWorld(worldIn, dim)
		val id = this.getUUIDFromAmbiguous(playerNameOrID)
		if (id != null) this.forWorld(world).addPlayer(chunk, id)
	}

	def removePlayer(worldIn: World, chunk: ChunkCoordIntPair, dim: Int,
			playerNameOrID: AnyRef): Unit = {
		val world = this.getWorld(worldIn, dim)
		val id = this.getUUIDFromAmbiguous(playerNameOrID)
		if (id != null) this.forWorld(world).removePlayer(chunk, id)
	}

	def isTemporaryChunk(worldIn: World, chunk: ChunkCoordIntPair, dim: Int): Boolean = {
		val chunkPair = new DimensionChunkPair(dim, chunk)
		for (dimChunk <- this.temporaryByUUID.values.flatten) if (dimChunk == chunkPair) return true
		false
	}

	def unForce(worldIn: World, chunk: ChunkCoordIntPair, dim: Int): Boolean = {
		if (this.unforceChunk(dim, chunk)) {
			if (!this.isTemporaryChunk(worldIn, chunk, dim))
				this.forWorld(this.getWorld(worldIn, dim)).removeChunk(chunk)
			else true
		} else false
	}

	def clearCacheForced(world: World, chunk: ChunkCoordIntPair, dim: Int): Unit = {
		this.forWorld(this.getWorld(world, dim)).clearCache(chunk)
	}

	override def shouldPlayerTicketPersist(playerName: String, ticket: Ticket): Boolean = {
		val uuid = this.getPlayerUUID(playerName)
		this.tempChunkPlayerOnlineOrTimeUnderMax(uuid, this.temporaryToStartTime(uuid))
	}

	override def shouldContinueForcingChunk(world: World, chunk: ChunkCoordIntPair): Boolean = {
		this.forWorld(world).contains(chunk)
	}

	def checkHourlyDelays(): Unit = {
		for (world <- DimensionManager.getWorlds) {
			this.forWorld(world).checkHourDelays(ModOptions.FORCED_MAX_TIME_AWAY_HOURS)
		}
	}

	def tempChunkPlayerOnlineOrTimeUnderMax(uuid: UUID, time: Long): Boolean = {
		Players.isOnline(uuid) || TimeUnit.MILLISECONDS.toMinutes(
			System.currentTimeMillis() - time) <= ModOptions.TEMP_MAX_TIME_AWAY_MINUTES
	}

	def clearTempChunks(): Unit = {
		this.temporaryByUUID.clear()
		this.temporaryToStartTime.clear()
	}

	def checkMinuteDelays(): Unit = {
		for (uuidAndTime <- this.temporaryToStartTime) {
			if (!this.tempChunkPlayerOnlineOrTimeUnderMax(uuidAndTime._1, uuidAndTime._2))
				this.unload(uuidAndTime._1)
		}
	}

	def playerLogOut(player: EntityPlayer): Unit = {
		val uuid = player.getGameProfile.getId
		if (uuid == null) return
		if (this.temporaryToStartTime.contains(uuid))
			this.temporaryToStartTime(uuid) = System.currentTimeMillis()
	}

	def playerLogIn(player: EntityPlayer): Unit = {
		val uuid = player.getGameProfile.getId
		if (uuid == null) return
		if (this.temporaryToStartTime.contains(uuid)) this.temporaryToStartTime(uuid) = 0L
	}

	def getAllForcedChunks(world: World): Array[ChunkCoordIntPair] = {
		this.getAllForcedChunks(this.forWorld(world))
	}

	def getAllForcedChunks(world: WorldDataChunks): Array[ChunkCoordIntPair] = {
		val forcedChunks = ListBuffer[ChunkCoordIntPair]()
		forcedChunks ++= world.getAllChunks
		for (chunkPair <- this.temporaryByUUID.values.flatten) {
			if (chunkPair.getDimension == world.getDimension) forcedChunks += chunkPair
		}
		forcedChunks.toArray
	}

}
