package temportalist.chunkcommander.server

import java.util

import net.minecraft.command.{WrongUsageException, ICommandSender, CommandBase}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.BlockPos
import net.minecraft.world.ChunkCoordIntPair
import temportalist.chunkcommander.common.CommandChunkLoader

import scala.collection.mutable.ListBuffer

/**
  * Created by TheTemportalist on 1/14/2016.
  */
object CommandChunk extends CommandBase {

	override def getCommandName: String = "chunk"

	override def getCommandUsage(sender: ICommandSender): String = "commands.chunk.usage"

	override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
		// todo add dimension to usage
		/* Usage:
		  -1    0     1         2        3...
		/chunk force <chunkX> <chunkY> [player]...
		/chunk force current [player]...
		/chunk load <chunkX> <chunkY> [player]
		/chunk load current [player]
		/chunk unload <chunkX> <chunkY> [player]
		/chunk unload current [player]
		/chunk player add <chunkX> <chunkY> <player>
		/chunk player add current <player>
		/chunk player remove <chunkX> <chunkY> <player>
		/chunk player remove current <player>
		/chunk unforce <chunkX> <chunkY>
		/chunk unforce current
		/chunk clearCache <chunkX> <chunkY>
		/chunk clearCache current
		 */

		def wrongUsage(suffix: String = null): Unit =
			throw new WrongUsageException("commands.chunk.usage" +
					(if (suffix == null) "" else "." + suffix))

		if (args.length <= 1) wrongUsage()
		val world = sender.getEntityWorld
		args(0) match {
			case "force" => //persistent loading
				if (args.length < 2) wrongUsage("force")
				val chunkRet = this.getChunk(sender, args, 1)
				val chunkCoord: ChunkCoordIntPair = chunkRet._1

				val playerNames = ListBuffer[String]()
				for (i <- chunkRet._2 until args.length) playerNames += args(i)

				CommandChunkLoader.forceWithPlayers(world, chunkCoord, playerNames: _*)
			case "load" => // temporary loading, player centered
				if (args.length < 2) wrongUsage("load")
				val chunkRet = this.getChunk(sender, args, 1)
				val chunkCoord: ChunkCoordIntPair = chunkRet._1
				val remainingStart = chunkRet._2

				val playerNameOrID = this.getPlayerNameOrID(sender, args, remainingStart)
				if (playerNameOrID == null) wrongUsage("load.player")

				CommandChunkLoader.load(world, chunkCoord, playerNameOrID)
			case "unload" => // temporary unloading, player centered
				if (args.length < 2) wrongUsage("unload")
				val chunkRet = this.getChunk(sender, args, 1)
				val chunkCoord: ChunkCoordIntPair = chunkRet._1
				val remainingStart = chunkRet._2

				val playerNameOrID = this.getPlayerNameOrID(sender, args, remainingStart)
				if (playerNameOrID == null) wrongUsage("unload.player")

				CommandChunkLoader.unload(world, chunkCoord, playerNameOrID)
			case "player" => // adding and remove players to a forced chunk
				if (args.length <= 3) wrongUsage("player")
				val chunkRet = this.getChunk(sender, args, 2)
				val chunkCoord: ChunkCoordIntPair = chunkRet._1
				val remainingStart = chunkRet._2

				val playerNameOrID = this.getPlayerNameOrID(sender, args, remainingStart)
				if (playerNameOrID == null) wrongUsage("player.player")

				args(1) match {
					case "add" => CommandChunkLoader.addPlayer(world, chunkCoord, playerNameOrID)
					case "remove" => CommandChunkLoader.removePlayer(world, chunkCoord, playerNameOrID)
					case _ =>
				}
			case "unforce" => // unforce loading a chunk
				if (args.length < 2) wrongUsage("unforce")
				CommandChunkLoader.unForce(world, this.getChunk(sender, args, 1)._1)
			case "clearCache" => // clear name cache for a chunk
				if (args.length <= 2) wrongUsage("clearCache")
				val chunkRet = this.getChunk(sender, args, 1)
				val chunkCoord: ChunkCoordIntPair = chunkRet._1
				CommandChunkLoader.clearCacheForced(world, chunkCoord)
			case _ => wrongUsage()
		}

	}

	def getChunk(sender: ICommandSender, args: Array[String], start: Int): (ChunkCoordIntPair, Int) = {
		args(start) match {
			case "current" =>
				(this.getCurrentChunk(sender), start + 1)
			case _ =>
				(new ChunkCoordIntPair(asInt(args(start)), asInt(args(start + 1))), start + 2)
		}
	}

	def getPlayerNameOrID(sender: ICommandSender, args: Array[String], start: Int): AnyRef = {
		if (start < args.length) args(start)
		else sender.getCommandSenderEntity match {
			case player: EntityPlayer => player.getGameProfile.getId
			case _ => null
		}
	}

	def asInt(str: String): Int = {
		CommandBase.parseInt(str)
	}

	override def addTabCompletionOptions(sender: ICommandSender, args: Array[String],
			pos: BlockPos): util.List[String] = {
		val userNames = MinecraftServer.getServer.getAllUsernames
		val matchingWords = args.length match {
			case 1 => Array[String](
				"force", "load", "unload", "player", "unforce", "clearCache"
			)
			case 2 => Array[String]("current")
			case 3 =>
				val list = ListBuffer[String]("current")
				list ++= (args(1) match {
					case "player" => Array[String]("add", "remove")
					case _ => Array[String]()
				})
				args(1) match {
					case "unforce" =>
					case "clearCache" =>
					case _ => list ++= userNames
				}
				list.toArray
			case _ => null
		}
		if (matchingWords != null)
			CommandBase.getListOfStringsMatchingLastWord(args, matchingWords: _*)
		else null
	}

	def getCurrentChunk(sender: ICommandSender): ChunkCoordIntPair = {
		val pos = sender.getPosition
		new ChunkCoordIntPair(pos.getX >> 4, pos.getZ >> 4)
	}

}
