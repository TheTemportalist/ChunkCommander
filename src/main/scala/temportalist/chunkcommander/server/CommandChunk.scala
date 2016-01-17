package temportalist.chunkcommander.server

import java.util

import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.server.MinecraftServer
import net.minecraft.util.{BlockPos, ChatComponentText, ChatComponentTranslation}
import net.minecraft.world.ChunkCoordIntPair
import temportalist.chunkcommander.common.CommandChunkLoader
import temportalist.origin.internal.server.command.ICommand

import scala.collection.mutable.ListBuffer

/**
  * Created by TheTemportalist on 1/14/2016.
  */
object CommandChunk extends CommandBase with ICommand {

	override def getCommandName: String = "chunk"

	override def getUsage: String = "commands.chunk.usage"

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

		if (args.length <= 1) wrongUsage()
		val world = sender.getEntityWorld
		args(0) match {
			case "force" => //persistent loading
				if (this.isBadOp(sender, 3)) return
				if (args.length < 2) wrongUsage("force")
				val chunkDimRet = this.getChunk(sender, args, 1)
				CommandChunkLoader.forceWithPlayers(world, chunkDimRet._1,
					chunkDimRet._2, (for (i <- chunkDimRet._3 until args.length) yield args(i)): _*)
			case "load" => // temporary loading, player centered
				if (this.isBadOp(sender, 2)) return
				if (args.length < 2) wrongUsage("load")
				val chunkDimRet = this.getChunk(sender, args, 1)
				val chunkCoord: ChunkCoordIntPair = chunkDimRet._1
				val remainingStart = chunkDimRet._3

				val playerNameOrID = this.getPlayerNameOrID(sender, args, remainingStart)
				if (playerNameOrID == null) wrongUsage("playerEr")

				CommandChunkLoader.load(world, chunkCoord, chunkDimRet._2, playerNameOrID)
			case "player" => // adding and remove players to a forced chunk
				if (this.isBadOp(sender, 3)) return
				if (args.length <= 3) wrongUsage("player")
				val chunkDimRet = this.getChunk(sender, args, 2)
				val remainingStart = chunkDimRet._3

				val playerNameOrID = this.getPlayerNameOrID(sender, args, remainingStart)
				if (playerNameOrID == null) wrongUsage("playerEr")

				args(1) match {
					case "add" => CommandChunkLoader.addPlayer(world, chunkDimRet._1,
						chunkDimRet._2, playerNameOrID)
					case "remove" => CommandChunkLoader.removePlayer(world, chunkDimRet._1,
						chunkDimRet._2, playerNameOrID)
					case _ =>
				}
			case "unforce" => // unforce loading a chunk
				if (args.length < 2) wrongUsage("unforce")
				val ret = this.getChunk(sender, args, 1)
				val isTempChunk = CommandChunkLoader.isTemporaryChunk(world, ret._1, ret._2)
				val canRun = this.canOpLevel(sender, 3) ||
						(isTempChunk && this.canOpLevel(sender, 2))
				if (!canRun) this.incorrectOp(sender, 2)
				else if (CommandChunkLoader.unForce(world, ret._1, ret._2)) {
					sender.addChatMessage(new ChatComponentText(
						(if (isTempChunk) "Unloaded" else "Unforced") + " chunk."))
				}
			case "clearCache" => // clear name cache for a chunk
				if (this.isBadOp(sender, 3)) return
				if (args.length < 2) wrongUsage("clearCache")
				val ret = this.getChunk(sender, args, 1)
				CommandChunkLoader.clearCacheForced(world, ret._1, ret._2)
			case "help" => // display information
				if (this.isBadOp(sender, 2)) return
				if (args.length < 2)
					sender.addChatMessage(new ChatComponentTranslation(
						"commands.chunk.usage.help.detail"))
				else wrongUsage(args(1))
			case _ => wrongUsage()
		}

	}

	def getPlayerNameOrID(sender: ICommandSender, args: Array[String], start: Int): AnyRef = {
		if (start < args.length) this.getPlayerProfile(sender, args(start), checkSender = false)
		else this.getPlayerProfile(sender, null)
	}

	override def getRequiredPermissionLevel: Int = 1

	def getChunk(sender: ICommandSender, args: Array[String], start: Int): (ChunkCoordIntPair, Int, Int) = {
		args(start) match {
			case "current" =>
				(this.getCurrentChunk(sender),
						sender.getEntityWorld.provider.getDimensionId, start + 1)
			case _ =>
				(new ChunkCoordIntPair(asInt(args(start)), asInt(args(start + 1))),
						args(start + 2).toInt, start + 3)
		}
	}

	override def addTabCompletionOptions(sender: ICommandSender, args: Array[String],
			pos: BlockPos): util.List[String] = {
		val userNames = MinecraftServer.getServer.getAllUsernames
		val cmds = Array[String](
			"force", "load", "player", "unforce", "clearCache", "help"
		)
		val matchingWords = args.length match {
			case 1 => cmds
			case 2 =>
				args(1) match {
					case "help" => cmds
					case _ => Array[String]("current")
				}
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
