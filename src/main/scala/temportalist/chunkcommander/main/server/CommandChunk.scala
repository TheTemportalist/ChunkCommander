package temportalist.chunkcommander.main.server

import java.util

import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.{TextComponentString, TextComponentTranslation}
import net.minecraft.world.ChunkCoordIntPair
import temportalist.chunkcommander.main.common.{ChunkCommander, ChunkLoaderCommand}
import temportalist.origin.api.common.IModDetails
import temportalist.origin.foundation.server.Command

/**
  *
  * Created by TheTemportalist on 4/13/2016.
  *
  * @author TheTemportalist
  */
object CommandChunk extends Command {

	override def getDetails: IModDetails = ChunkCommander

	override def getCommandName: String = "chunk"

	override def execute(server: MinecraftServer, sender: ICommandSender,
			args: Array[String]): Unit = {

		// todo add dimension to usage
		/* Usage:
		  -1      0            1                    2           3
		/chunk  force       <chunkX>:<chunkY>   [player*]
		/chunk  force       current             [player*]

		/chunk  load        <chunkX>:<chunkY>   <player>
		/chunk  load        current             <player>

		/chunk  player      add                 <player>    <chunkX>:<chunkY>
		/chunk  player      add                 <player>    current
		/chunk  player      remove              <player>    <chunkX>:<chunkY>
		/chunk  player      remove              <player>    current

		/chunk  unforce     <chunkX>:<chunkY>   [player]
		/chunk  unforce     current             [player]

		/chunk  clearCache  <chunkX>:<chunkY>
		/chunk  clearCache  current
		 */

		if (args.length < 2) {
			wrongUsage()
			return
		}

		val world = sender.getEntityWorld
		args(0) match {
			case "force" =>
				/*
				/chunk  force       <chunkX>:<chunkY>   [player*]
				/chunk  force       current             [player*]
				 */

				/*
				if (this.isBadOp(sender, 3)) {
					wrongUsage("Invalid permissions")
					return
				}
				*/
				if (args.length < 2) {
					wrongUsage("force")
					return
				}

				val chunkDimReturn = this.getChunk(sender, args, 1)
				ChunkLoaderCommand.forceWithPlayers(world, chunkDimReturn._1, chunkDimReturn._2,
					(for (i <- chunkDimReturn._3 until args.length) yield args(i)):_*
				)

			case "load" =>
				/*
				/chunk  load        <chunkX>:<chunkY>   <player>
				/chunk  load        current             <player>
				 */

				/*
				if (this.isBadOp(sender, 2)) {
					wrongUsage()
					return
				}
				*/
				if (args.length < 2) {
					wrongUsage("load")
					return
				}

				val chunkDimReturn = this.getChunk(sender, args, 1)
				ChunkLoaderCommand.load(world, chunkDimReturn._1, chunkDimReturn._2, args(2))

			case "unforce" =>
				/*
				/chunk  unforce     <chunkX>:<chunkY>   [player]
				/chunk  unforce     current             [player]
				 */

				/*
				if (this.isBadOp(sender, 3)) {
					wrongUsage("Invalid permissions")
					return
				}
				*/
				if (args.length < 2) {
					wrongUsage("unforce")
					return
				}

				val chunkDimReturn = this.getChunk(sender, args, 1)
				val isTempChunk = ChunkLoaderCommand.isTemporaryChunk(world, chunkDimReturn._1, chunkDimReturn._2)
				val canRun = true//this.canOpLevel(sender, 3) || (isTempChunk && this.canOpLevel(sender, 2))
				if (!canRun) this.incorrectOp(sender, 2)
				else if (isTempChunk) {
					if (args.length > 2) {
						val playerName = args(2)
						if (ChunkLoaderCommand.unload(world, chunkDimReturn._1, playerName)) {
							sender.addChatMessage(new TextComponentString("Unloaded chunk."))
							return
						}
					}

					sender.addChatMessage(new TextComponentString("Could not unload chunk."))
				}
				else if (ChunkLoaderCommand.unForce(world, chunkDimReturn._1, chunkDimReturn._2)) {
					sender.addChatMessage(new TextComponentString("Unforced chunk."))
				}

			case "player" =>
				/*
				/chunk  player      add                 <player>    <chunkX>:<chunkY>
				/chunk  player      add                 <player>    current
				/chunk  player      remove              <player>    <chunkX>:<chunkY>
				/chunk  player      remove              <player>    current
				 */

				/*
				if (this.isBadOp(sender, 3)) {
					wrongUsage("Invalid permissions")
					return
				}
				*/
				if (args.length < 4) {
					wrongUsage("player")
					return
				}

				val playerName = args(2)
				val chunkDimRet = this.getChunk(sender, args, 3)
				args(1) match {
					case "add" => ChunkLoaderCommand.addPlayer(world, chunkDimRet._1,
						chunkDimRet._2, playerName)
					case "remove" => ChunkLoaderCommand.removePlayer(world, chunkDimRet._1,
						chunkDimRet._2, playerName)
					case _ =>
				}
			case "clearCache" =>
				/*
				/chunk  clearCache  <chunkX>:<chunkY>
				/chunk  clearCache  current
				 */

				/*
				if (this.isBadOp(sender, 3)) {
					wrongUsage("Invalid permissions")
					return
				}
				*/
				if (args.length < 2) {
					wrongUsage("clearCache")
					return
				}

				val ret = this.getChunk(sender, args, 1)
				ChunkLoaderCommand.clearCacheForced(world, ret._1, ret._2)
			case "help" =>
				/*
				if (this.isBadOp(sender, 0)) {
					wrongUsage("Invalid permissions")
					return
				}
				*/

				if (args.length < 2) sender.addChatMessage(new TextComponentTranslation(this.getUsage + ".help.detail"))
				else wrongUsage(args(1))
			case _ => wrongUsage()
		}


	}

	override def getTabCompletionOptions(server: MinecraftServer, sender: ICommandSender,
			args: Array[String], pos: BlockPos): util.List[String] = {
		args.length match {
			case 1 => tabCompleteSet(args, "force", "load", "unforce", "player", "clearCache")
			case _ =>
				val superTab = super.getTabCompletionOptions(server, sender, args, pos)
				if (Array[String]("force", "load").contains(args(0))) {
					args.length match {
						case 2 => tabCompleteSet(args, "current")
						case 3 => tabCompleteUsername(args, server)
						case _ => superTab
					}
				}
				else if (Array[String]("unforce", "clearCache").contains(args(0))) {
					args.length match {
						case 2 => tabCompleteSet(args, "current")
						case _ => superTab
					}
				}
				else if ("player" == args(0)) {
					args.length match {
						case 2 => tabCompleteSet(args, "add", "remove")
						case 3 => tabCompleteUsername(args, server)
						case 4 => tabCompleteSet(args, "current")
						case _ => superTab
					}
				}
				else superTab
		}
	}

	def tabCompleteSet(args: Array[String], options: String*): util.List[String] = {
		CommandBase.getListOfStringsMatchingLastWord(args, options:_*)
	}

	def tabCompleteUsername(args: Array[String], server: MinecraftServer): util.List[String] = {
		this.tabCompleteSet(args, server.getPlayerList.getAllUsernames:_*)
	}

	def getChunk(sender: ICommandSender, args: Array[String], start: Int): (ChunkCoordIntPair, Int, Int) = {
		args(start) match {
			case "current" =>
				(this.getCurrentChunk(sender),
						sender.getEntityWorld.provider.getDimension, start + 1)
			case _ =>
				(new ChunkCoordIntPair(asInt(args(start)), asInt(args(start + 1))),
						args(start + 2).toInt, start + 3)
		}
	}

	def getCurrentChunk(sender: ICommandSender): ChunkCoordIntPair = {
		val pos = sender.getPosition
		new ChunkCoordIntPair(pos.getX >> 4, pos.getZ >> 4)
	}

	override def getRequiredPermissionLevel: Int = 1

}
