package temportalist.chunkcommander.main.common.network

import net.minecraft.util.math.ChunkPos
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}
import net.minecraftforge.fml.relauncher.Side
import temportalist.chunkcommander.main.client.WorldRender
import temportalist.origin.foundation.common.network.IPacket

/**
  *
  * Created by TheTemportalist on 4/13/2016.
  *
  * @author TheTemportalist
  */
class PacketChunk_Client extends IPacket {

	def this(function: Byte, chunks: ChunkPos*) {
		this()
		this.add(function)
		this.add(chunks.toArray)
	}

	override def getReceivableSide: Side = Side.CLIENT

}
object PacketChunk_Client {
	class Handler extends IMessageHandler[PacketChunk_Client, IMessage] {
		override def onMessage(message: PacketChunk_Client, ctx: MessageContext): IMessage = {
			val function = message.get[Byte]
			val set = message.get[Array[ChunkPos]]
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
