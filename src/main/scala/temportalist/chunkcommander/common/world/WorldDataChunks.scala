package temportalist.chunkcommander.common.world

import java.util.UUID
import java.util.concurrent.TimeUnit

import net.minecraft.nbt.{NBTTagCompound, NBTTagList}
import net.minecraft.world.ChunkCoordIntPair
import temportalist.chunkcommander.common.network.PacketChunk_Client
import temportalist.chunkcommander.common.{ChunkCommander, CommandChunkLoader}
import temportalist.origin.api.common.utility.{NBTHelper, Players}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by TheTemportalist on 1/16/2016.
  */
class WorldDataChunks(key: String) extends WorldDataHandler.WorldData(key) {

	private val chunks = ListBuffer[ChunkCoordIntPair]()
	private val chunkToPlayers = mutable.Map[ChunkCoordIntPair, ListBuffer[UUID]]()
	private val chunkToStartTime = mutable.Map[ChunkCoordIntPair, Long]()
	private val cache = mutable.Map[ChunkCoordIntPair, ListBuffer[UUID]]()

	def addChunk(chunk: ChunkCoordIntPair, players: Array[UUID], startTime: Long): Unit = {
		this.chunks += chunk

		if (this.chunkToPlayers.contains(chunk)) this.chunkToPlayers(chunk).clear()
		else this.chunkToPlayers(chunk) = ListBuffer[UUID]()
		this.chunkToPlayers(chunk) ++= players

		this.chunkToStartTime(chunk) = startTime

		this.cache(chunk) = ListBuffer[UUID]()
		this.cache(chunk) ++= players

		this.markDirty()
	}

	def removeChunk(chunk: ChunkCoordIntPair): Boolean = {
		if (this contains chunk) {
			this.chunks -= chunk
			this.cache(chunk) = this.chunkToPlayers.remove(chunk).getOrElse(ListBuffer[UUID]())
			this.chunkToStartTime.remove(chunk)
			this.markDirty()
			true
		} else false
	}

	def contains(chunk: ChunkCoordIntPair): Boolean = this.chunks contains chunk

	def addPlayer(chunk: ChunkCoordIntPair, id: UUID): Unit = {
		if (this contains chunk) {
			this.chunkToPlayers(chunk) += id
			this.markDirty()
		}
	}

	def removePlayer(chunk: ChunkCoordIntPair, id: UUID): Unit = {
		if (this contains chunk) {
			this.chunkToPlayers(chunk) -= id
			this.markDirty()
		}
	}

	def clearCache(chunk: ChunkCoordIntPair): Unit = this.cache.remove(chunk)

	def checkHourDelays(max: Long): Unit = {
		var didChange = false
		for (chunkAndStart <- this.chunkToStartTime) {
			if (!{
				var hasOnlinePlayer = false
				for (uuid <- this.chunkToPlayers(chunkAndStart._1))
					if (!hasOnlinePlayer) hasOnlinePlayer = Players.isOnline(uuid)

				hasOnlinePlayer ||
						TimeUnit.MILLISECONDS.toHours(
							System.currentTimeMillis() - chunkAndStart._2) <= max
			}) {
				if (CommandChunkLoader.unforceChunk(this.getDimension, chunkAndStart._1)) {
					this.removeChunk(chunkAndStart._1)
					didChange = true
				}
			}
		}
		if (didChange) this.markDirty()
	}

	def getAllChunks: Array[ChunkCoordIntPair] = this.chunks.toArray

	override def markDirty(): Unit = {
		super.markDirty()
		this.updateDimension()
	}

	def updateDimension(): Unit = {
		new PacketChunk_Client(0, CommandChunkLoader.getAllForcedChunks(this): _*).sendToDimension(
			ChunkCommander, this.getDimension)
	}

	override def write(nbt: NBTTagCompound): Unit = {
		val chunkList = new NBTTagList
		for (chunk <- this.chunks) {
			val tagCom = new NBTTagCompound

			tagCom.setInteger("x", chunk.chunkXPos)
			tagCom.setInteger("z", chunk.chunkZPos)
			tagCom.setTag("ids", NBTHelper.asTag(this.chunkToPlayers(chunk)))

			chunkList.appendTag(tagCom)
		}
		nbt.setTag("chunks", chunkList)
	}

	override def read(nbt: NBTTagCompound): Unit = {
		this.chunks.clear()
		this.chunkToPlayers.clear()
		this.chunkToStartTime.clear()
		this.cache.clear()

		val chunkList = nbt.getTagList("chunks", NBTHelper.getNBTType[NBTTagCompound])
		for (i <- 0 until chunkList.tagCount()) {
			val tagCom = chunkList.getCompoundTagAt(i)
			val chunk = new ChunkCoordIntPair(tagCom.getInteger("x"), tagCom.getInteger("z"))
			this.chunks += chunk
			this.chunkToPlayers(chunk) = NBTHelper.get[ListBuffer[UUID]](tagCom.getTag("ids"))
			this.chunkToStartTime(chunk) = System.currentTimeMillis()
		}

	}

}
