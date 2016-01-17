package temportalist.chunkcommander.common

import java.util

import com.google.common.collect.{LinkedListMultimap, ListMultimap}
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.world.{ChunkCoordIntPair, World}
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.ForgeChunkManager._
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent, FMLServerStoppingEvent}
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.{PlayerChangedDimensionEvent, PlayerLoggedInEvent, PlayerLoggedOutEvent}
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent
import net.minecraftforge.fml.common.{Mod, SidedProxy}
import net.minecraftforge.fml.relauncher.Side
import temportalist.chunkcommander.api.ApiChunkLoading
import temportalist.chunkcommander.common.network.PacketChunk_Client
import temportalist.chunkcommander.common.world.{WorldDataChunks, WorldDataHandler}
import temportalist.chunkcommander.server.CommandChunk
import temportalist.origin.api.common.resource.{EnumResource, IModDetails, IModResource}
import temportalist.origin.api.common.utility.WorldHelper
import temportalist.origin.foundation.common.IMod
import temportalist.origin.foundation.common.proxy.IProxy
import temportalist.origin.foundation.common.register.Registry

import scala.collection.mutable.ListBuffer
import scala.collection.{JavaConversions, mutable}

/**
  * Created by TheTemportalist on 1/14/2016.
  */
@Mod(modid = ChunkCommander.MOD_ID, name = ChunkCommander.MOD_NAME,
	version = ChunkCommander.MOD_VERSION, modLanguage = "scala",
	guiFactory = ChunkCommander.proxyClient,
	dependencies = "required-after:origin"
)
object ChunkCommander extends IMod with IModResource {

	// ~~~~~~~~~~~ Mod Setup ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	final val MOD_ID = "chunkcommander"
	final val MOD_NAME = "Chunk Commander"
	final val MOD_VERSION = "1.0.0"

	override def getModID: String = this.MOD_ID

	override def getModVersion: String = this.MOD_VERSION

	override def getModName: String = this.MOD_NAME

	override def getDetails: IModDetails = this

	// ~~~~~~~~~~~ Proxy ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	final val proxyClient = "temportalist.chunkcommander.client.ProxyClient"
	final val proxyServer = "temportalist.chunkcommander.server.ProxyServer"

	@SidedProxy(clientSide = proxyClient, serverSide = proxyServer)
	var proxy: IProxy = null

	// ~~~~~~~~~~~ Inits ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Mod.EventHandler
	def preInit(event: FMLPreInitializationEvent): Unit = {
		super.preInitialize(this, event, this.proxy, null)
		ApiChunkLoading.register(CommandChunkLoader)
		this.registerNetwork()
		this.registerPacket(classOf[PacketChunk_Client.Handler],
			classOf[PacketChunk_Client], Side.CLIENT)
		Registry.registerHandler(ChunkServer)
		WorldDataHandler.register(classOf[WorldDataChunks], this.getModID + "_data")

	}

	@Mod.EventHandler
	def init(event: FMLInitializationEvent): Unit = {
		super.initialize(event, this.proxy)
	}

	@Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent): Unit = {
		super.postInitialize(event, this.proxy)
		JavaConversions.asScalaSet(ApiChunkLoading.getRegisteredMods).foreach(mod => {
			ForgeChunkManager.setForcedChunkLoadingCallback(mod, LoadingCallback)
		})
		Registry.registerCommand(CommandChunk)
		this.loadResource("chunkFace", (EnumResource.TEXTURE, "world/white.png"))
	}

	// ~~~~~~~~~~~ Chunk Management ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	object LoadingCallback extends LoadingCallback with OrderedLoadingCallback
			with PlayerOrderedLoadingCallback {

		/**
		  * Called back when tickets are loaded from the world to allow the
		  * mod to re-register the chunks associated with those tickets. The list supplied
		  * here is truncated to length prior to use. Tickets unwanted by the
		  * mod must be disposed of manually unless the mod is an OrderedLoadingCallback instance
		  * in which case, they will have been disposed of by the earlier callback.
		  *
		  * @param tickets The tickets to re-register. The list is immutable and cannot be manipulated directly. Copy it first.
		  * @param world the world
		  */
		override def ticketsLoaded(tickets: util.List[Ticket], world: World): Unit = {
			val buffer: mutable.Buffer[ForgeChunkManager.Ticket] =
				JavaConversions.asScalaBuffer(tickets)
			for (ticket <- buffer) {
				ChunkCommander.notifyLoaderWithTicket(ticket)
				for (chunk <- JavaConversions.asScalaIterator(ticket.getChunkList.iterator())) {
					if (ChunkCommander.shouldContinueForcingChunk(
							ticket.getModId, ticket.world, chunk))
						ForgeChunkManager.forceChunk(ticket, chunk)
					else ForgeChunkManager.releaseTicket(ticket)
				}
			}
		}

		/**
		  * Called back when tickets are loaded from the world to allow the
		  * mod to decide if it wants the ticket still, and prioritise overflow
		  * based on the ticket count.
		  * WARNING: You cannot force chunks in this callback, it is strictly for allowing the mod
		  * to be more selective in which tickets it wishes to preserve in an overflow situation
		  *
		  * @param tickets The tickets that you will want to select from. The list is immutable and cannot be manipulated directly. Copy it first.
		  * @param world The world
		  * @param maxTicketCount The maximum number of tickets that will be allowed.
		  * @return A list of the tickets this mod wishes to continue using. This list will be truncated
		  * to "maxTicketCount" size after the call returns and then offered to the other callback
		  * method
		  */
		override def ticketsLoaded(tickets: util.List[Ticket], world: World,
				maxTicketCount: Int): util.List[Ticket] = {
			val ticketsToBeLoaded = ListBuffer[Ticket]()
			// todo how to order
			ticketsToBeLoaded ++= JavaConversions.asScalaBuffer(tickets)
			JavaConversions.bufferAsJavaList(ticketsToBeLoaded)
		}

		override def playerTicketsLoaded(tickets: ListMultimap[String, Ticket],
				world: World): ListMultimap[String, Ticket] = {
			val map = LinkedListMultimap.create[String, Ticket]()
			val ticketMap = JavaConversions.mapAsScalaMap(tickets.asMap())
			for (entry <- ticketMap)
				for (ticket <- JavaConversions.collectionAsScalaIterable(entry._2)) {
					//if (ChunkCommander.shouldPlayerTicketPersist(entry._1, ticket))
					//	map.put(entry._1, ticket)
					ForgeChunkManager.releaseTicket(ticket)
				}
			map
		}

	}

	def notifyLoaderWithTicket(ticket: Ticket): Unit = {
		ApiChunkLoading.getChunkLoader(ticket.getModId).notifyWithTicket(ticket)
	}

	def shouldContinueForcingChunk(modID: String, world: World, chunk: ChunkCoordIntPair): Boolean = {
		ApiChunkLoading.getChunkLoader(modID).shouldContinueForcingChunk(world, chunk)
	}

	def shouldPlayerTicketPersist(playerName: String, ticket: Ticket): Boolean = {
		ApiChunkLoading.getChunkLoader(
			ticket.getModId).shouldPlayerTicketPersist(playerName, ticket)
	}

	private var nextCheck_Hour: Long = 0L
	private var nextCheck_Minute: Long = 0L
	@SubscribeEvent
	def ticker(event: ServerTickEvent): Unit = {
		if (event.phase == TickEvent.Phase.START) {
			// todo move time vars to static place somewhere
			if (System.currentTimeMillis() > nextCheck_Hour) {
				CommandChunkLoader.checkHourlyDelays()
				nextCheck_Hour = System.currentTimeMillis() + 3600000 // next hour
			}
			if (System.currentTimeMillis() > nextCheck_Minute) {
				CommandChunkLoader.checkMinuteDelays()
				nextCheck_Minute = System.currentTimeMillis() + 60000 // next hour
			}
		}
	}

	@SubscribeEvent
	def playerLogIn(event: PlayerLoggedInEvent): Unit = CommandChunkLoader.playerLogIn(event.player)

	@SubscribeEvent
	def playerLogOut(event: PlayerLoggedOutEvent): Unit = CommandChunkLoader.playerLogOut(event.player)

	object ChunkServer {
		@SubscribeEvent
		def playerLogIn(event: PlayerLoggedInEvent): Unit = {
			if (WorldHelper.isClient(event.player.getEntityWorld)) return
			this.updatePlayer(event.player.asInstanceOf[EntityPlayerMP])
		}

		@SubscribeEvent
		def playerChangeDim(event: PlayerChangedDimensionEvent): Unit = {
			if (WorldHelper.isClient(event.player.getEntityWorld)) return
			this.updatePlayer(event.player.asInstanceOf[EntityPlayerMP])
		}

		def updatePlayer(player: EntityPlayerMP): Unit = {
			// make sure chunks have been read for this world
			WorldDataHandler.forWorld[WorldDataChunks](player.getEntityWorld)
			// update the client
			new PacketChunk_Client(0, CommandChunkLoader.getAllForcedChunks(
				player.getEntityWorld): _*).sendToPlayer(
				ChunkCommander, player)
		}

		@SubscribeEvent
		def chunkChange(event: ForceChunkEvent): Unit = {
			if (WorldHelper.isClient(event.ticket.world)) return
			new PacketChunk_Client(1, event.location).sendToDimension(
				ChunkCommander, event.ticket.world.provider.getDimensionId)
		}

		@SubscribeEvent
		def chunkChange(event: UnforceChunkEvent): Unit = {
			if (WorldHelper.isClient(event.ticket.world)) return
			new PacketChunk_Client(2, event.location).sendToDimension(
				ChunkCommander, event.ticket.world.provider.getDimensionId)
		}

	}

	@Mod.EventHandler
	def serverStopping(event: FMLServerStoppingEvent): Unit = {
		CommandChunkLoader.clearTempChunks()
	}

}
