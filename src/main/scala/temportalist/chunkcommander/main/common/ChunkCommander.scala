package temportalist.chunkcommander.main.common

import java.util

import com.google.common.collect.{LinkedListMultimap, ListMultimap}
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
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
import temportalist.chunkcommander.main.common.network.PacketChunk_Client
import temportalist.chunkcommander.main.common.world.{WorldDataChunks, WorldDataHandler}
import temportalist.chunkcommander.main.server.CommandChunk
import temportalist.origin.api.common.{EnumResource, IModResource}
import temportalist.origin.foundation.common.modTraits.IHasCommands
import temportalist.origin.foundation.common.registers.OptionRegister
import temportalist.origin.foundation.common.{IProxy, ModBase}
import temportalist.origin.foundation.server.ICommand

import scala.collection.mutable.ListBuffer
import scala.collection.{JavaConversions, mutable}

/**
  *
  * Created by TheTemportalist on 1/14/2016.
  *
  * @author TheTemportalist
  */
@Mod(modid = ChunkCommander.MOD_ID, name = ChunkCommander.MOD_NAME, version = ChunkCommander.MOD_VERSION,
	modLanguage = "scala",
	guiFactory = ChunkCommander.proxyClient,
	dependencies = "required-after:Forge;required-after:Origin"
)
object ChunkCommander extends ModBase with IHasCommands with IModResource {

	final val MOD_ID = "chunkcommander"
	final val MOD_NAME = "Chunk Commander"
	final val MOD_VERSION = "@MOD_VERSION@"
	final val proxyClient = "temportalist.chunkcommander.main.client.ProxyClient"
	final val proxyServer = "temportalist.chunkcommander.main.server.ProxyServer"

	/**
	  *
	  * @return A mod's ID
	  */
	override def getModId: String = this.MOD_ID

	/**
	  *
	  * @return A mod's name
	  */
	override def getModName: String = this.MOD_NAME

	/**
	  *
	  * @return A mod's version
	  */
	override def getModVersion: String = this.MOD_VERSION

	@SidedProxy(clientSide = proxyClient, serverSide = proxyServer)
	var proxy: IProxy = _

	override def getProxy: IProxy = this.proxy

	override def getOptions: OptionRegister = Options

	@Mod.EventHandler
	def preInit(event: FMLPreInitializationEvent): Unit = {
		super.preInitialize(event)

		ApiChunkLoading.register(ChunkLoaderCommand)

		this.registerNetwork()
		this.registerMessage(classOf[PacketChunk_Client.Handler],
			classOf[PacketChunk_Client], Side.CLIENT)

		this.registerHandler(ChunkServer)
		WorldDataHandler.register(classOf[WorldDataChunks], this.getModId + "_data")

	}

	@Mod.EventHandler
	def init(event: FMLInitializationEvent): Unit = {
		super.initialize(event)

	}

	@Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent): Unit = {
		super.postInitialize(event)

		JavaConversions.asScalaSet(ApiChunkLoading.getRegisteredMods).foreach(mod => {
			ForgeChunkManager.setForcedChunkLoadingCallback(mod, LoadingCallback)
		})

		this.loadResource("chunkFace", (EnumResource.TEXTURE, "world/white.png"))
	}

	override def getCommands: Seq[ICommand] = Seq(CommandChunk)

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

	def shouldContinueForcingChunk(modID: String, world: World, chunk: ChunkPos): Boolean = {
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
			if (System.currentTimeMillis() > nextCheck_Hour) {
				ChunkLoaderCommand.checkHourlyDelays()
				nextCheck_Hour = System.currentTimeMillis() + 3600000 // next hour
			}
			if (System.currentTimeMillis() > nextCheck_Minute) {
				ChunkLoaderCommand.checkMinuteDelays()
				nextCheck_Minute = System.currentTimeMillis() + 60000 // next hour
			}
		}
	}

	@SubscribeEvent
	def playerLogIn(event: PlayerLoggedInEvent): Unit = ChunkLoaderCommand.playerLogIn(event.player)

	@SubscribeEvent
	def playerLogOut(event: PlayerLoggedOutEvent): Unit = ChunkLoaderCommand.playerLogOut(event.player)

	object ChunkServer {
		@SubscribeEvent
		def playerLogIn(event: PlayerLoggedInEvent): Unit = {
			if (event.player.getEntityWorld.isRemote) return
			this.updatePlayer(event.player.asInstanceOf[EntityPlayerMP])
		}

		@SubscribeEvent
		def playerChangeDim(event: PlayerChangedDimensionEvent): Unit = {
			if (event.player.getEntityWorld.isRemote) return
			this.updatePlayer(event.player.asInstanceOf[EntityPlayerMP])
		}

		def updatePlayer(player: EntityPlayerMP): Unit = {
			// make sure chunks have been read for this world
			WorldDataHandler.forWorld[WorldDataChunks](player.getEntityWorld)
			// update the client
			new PacketChunk_Client(0,
				ChunkLoaderCommand.getAllForcedChunks(player.getEntityWorld): _*).
					sendToPlayer(ChunkCommander, player)
		}

		@SubscribeEvent
		def chunkChange(event: ForceChunkEvent): Unit = {
			if (event.getTicket.world.isRemote) return
			new PacketChunk_Client(1, event.getLocation).sendToDimension(
				ChunkCommander, event.getTicket.world.provider.getDimension)
		}

		@SubscribeEvent
		def chunkChange(event: UnforceChunkEvent): Unit = {
			if (event.getTicket.world.isRemote) return
			new PacketChunk_Client(2, event.getLocation).sendToDimension(
				ChunkCommander, event.getTicket.world.provider.getDimension)
		}

	}

	@Mod.EventHandler
	def serverStopping(event: FMLServerStoppingEvent): Unit = {
		ChunkLoaderCommand.clearTempChunks()
	}

}
