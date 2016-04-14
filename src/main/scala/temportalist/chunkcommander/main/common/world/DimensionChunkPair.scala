package temportalist.chunkcommander.main.common.world

import net.minecraft.world.{World, ChunkCoordIntPair}

/**
  *
  * Created by TheTemportalist on 1/16/2016.
  *
  * @author TheTemportalist
  */
class DimensionChunkPair(private val dimension: Int, x: Int, z: Int)
		extends ChunkCoordIntPair(x, z) {

	def this(dim: Int, chunk: ChunkCoordIntPair) {
		this(dim, chunk.chunkXPos, chunk.chunkZPos)
	}

	def this(world: World, chunk: ChunkCoordIntPair) {
		this(world.provider.getDimension, chunk)
	}

	def getDimension: Int = this.dimension

	override def equals(any: Any): Boolean = {
		any match {
			case dimPair: DimensionChunkPair =>
				this.getDimension == dimPair.getDimension &&
						this.chunkXPos == dimPair.chunkXPos &&
						this.chunkZPos == dimPair.chunkZPos
			case _ => false
		}
	}

	override def hashCode(): Int = {
		var ret = 1
		ret = 37 * ret + this.dimension
		ret = 37 * ret + super.hashCode()
		ret
	}
}
