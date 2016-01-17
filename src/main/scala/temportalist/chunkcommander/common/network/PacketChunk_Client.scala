package temportalist.chunkcommander.common.network

import net.minecraft.world.ChunkCoordIntPair
import net.minecraftforge.fml.common.network.simpleimpl.{MessageContext, IMessage, IMessageHandler}
import net.minecraftforge.fml.relauncher.Side
import temportalist.chunkcommander.client.WorldRender
import temportalist.origin.foundation.common.network.IPacket

/**
  * Created by TheTemportalist on 1/15/2016.
  */
class PacketChunk_Client extends IPacket {

	def this(function: Byte, chunks: ChunkCoordIntPair*) {
		this()
		this.add(function)
		this.add(chunks)
		println("Sending " + chunks.size + " to client")
	}

	override def getReceivableSide: Side = Side.CLIENT

}
object PacketChunk_Client {
	class Handler extends IMessageHandler[PacketChunk_Client, IMessage] {
		override def onMessage(message: PacketChunk_Client, ctx: MessageContext): IMessage = {
			val function = message.get[Byte]
			val set = message.get[Array[ChunkCoordIntPair]]
			function match {
				case 0 => // send an array of chunks
					WorldRender.setForcedChunks(set)
				case 1 => // send a single chunk for adding
					WorldRender.addForcedChunk(set.head)
				case 2 => // send a single chunk for removal
					WorldRender.removeForcedChunk(set.head)
				case _ =>
			}
			null
		}
	}
}
