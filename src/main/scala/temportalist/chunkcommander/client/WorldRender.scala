package temportalist.chunkcommander.client

import java.awt.Color

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.{GlStateManager, Tessellator}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.world.{World, ChunkCoordIntPair}
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.opengl.GL11
import temportalist.chunkcommander.common.ChunkCommander
import temportalist.origin.api.client.utility.Rendering

import scala.collection.mutable.ListBuffer

/**
  * Created by TheTemportalist on 1/15/2016.
  */
@SideOnly(Side.CLIENT)
object WorldRender {

	private var shouldShowBoundaries = false
	private var showChunkBoundariesState = 0

	def toggleLoadedBoundaries(): Unit = this.shouldShowBoundaries = !this.shouldShowBoundaries

	def toggleChunkBoundariesAndCorners(): Unit = {
		showChunkBoundariesState += 1
		if (showChunkBoundariesState >= 3) this.showChunkBoundariesState = 0
	}

	private val forcedChunks = ListBuffer[ChunkCoordIntPair]()

	def setForcedChunks(set: Array[ChunkCoordIntPair]): Unit = {
		this.forcedChunks.clear()
		this.forcedChunks ++= set
	}

	def addForcedChunk(chunk: ChunkCoordIntPair): Unit = {
		this.forcedChunks += chunk
	}

	def removeForcedChunk(chunk: ChunkCoordIntPair): Unit = {
		this.forcedChunks -= chunk
	}

	def isChunkLoaded(world: World, chunk: ChunkCoordIntPair): Boolean = {
		world.getChunkProvider.chunkExists(chunk.chunkXPos, chunk.chunkZPos) &&
				!world.getChunkProvider.provideChunk(chunk.chunkXPos, chunk.chunkZPos).isEmpty
	}

	@SubscribeEvent
	def worldRenderLast(event: RenderWorldLastEvent): Unit = {
		val p = Minecraft.getMinecraft.thePlayer
		val world = Minecraft.getMinecraft.theWorld
		val partialTick = event.partialTicks

		GlStateManager.pushMatrix()
		GlStateManager.translate(
			-(p.lastTickPosX + (p.posX - p.lastTickPosX) * partialTick),
			-(p.lastTickPosY + (p.posY - p.lastTickPosY) * partialTick),
			-(p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * partialTick)
		)

		// Render general chunk boundaries
		if (this.showChunkBoundariesState > 0) {
			this.renderChunkCornerWithColor(world, p.chunkCoordX, p.chunkCoordZ, p.posY, 0.9f, 0F, 0F)
			if (showChunkBoundariesState == 2) {
				this.renderChunkBoundaryWithColor(world, p.chunkCoordX, p.chunkCoordZ, p.posY, 0F, 0.9F, 0F)
			}
		}

		// render loaded chunk boundaries
		if (this.shouldShowBoundaries) {
			GlStateManager.pushMatrix()
			val posY = p.posY
			for (chunk <- this.forcedChunks) if (isChunkLoaded(world, chunk)) {
				val chunkX = chunk.chunkXPos
				val chunkZ = chunk.chunkZPos

				// Order: SWNE
				val doRender =
					for (face <- EnumFacing.HORIZONTALS) yield !this.isForceLoaded(chunk, face)

				val xStart = chunkX << 4
				val zStart = chunkZ << 4
				val xEnd = xStart + 16
				val zEnd = zStart + 16
				val y = this.calculateYForHeight(world, posY, 32)
				val yStart = y._1
				val yEnd = y._2

				startLine()
				GlStateManager.color(0F, 0F, 0.9F, 1F)
				// Horizontal
				for (y <- yStart.toInt to yEnd.toInt) {
					if (doRender(0)) {
						// South
						GL11.glVertex3d(xStart, y, zEnd)
						GL11.glVertex3d(xEnd, y, zEnd)
					}
					if (doRender(1)) {
						// West
						GL11.glVertex3d(xStart, y, zStart)
						GL11.glVertex3d(xStart, y, zEnd)
					}
					if (doRender(2)) {
						// North
						GL11.glVertex3d(xStart, y, zStart)
						GL11.glVertex3d(xEnd, y, zStart)
					}
					if (doRender(3)) {
						// East
						GL11.glVertex3d(xEnd, y, zStart)
						GL11.glVertex3d(xEnd, y, zEnd)
					}
				}
				// Vertical
				for (h <- 1 to 15) {
					if (doRender(0)) {
						// South
						GL11.glVertex3d(xStart + h, yStart, zEnd)
						GL11.glVertex3d(xStart + h, yEnd, zEnd)
					}
					if (doRender(1)) {
						// West
						GL11.glVertex3d(xStart, yStart, zStart + h)
						GL11.glVertex3d(xStart, yEnd, zStart + h)
					}
					if (doRender(2)) {
						// North
						GL11.glVertex3d(xStart + h, yStart, zStart)
						GL11.glVertex3d(xStart + h, yEnd, zStart)
					}
					if (doRender(3)) {
						// East
						GL11.glVertex3d(xEnd, yStart, zStart + h)
						GL11.glVertex3d(xEnd, yEnd, zStart + h)
					}
				}
				endLine()

			}
			GlStateManager.popMatrix()
		}

		GlStateManager.popMatrix()

	}

	def startLine(): Unit = {
		GlStateManager.pushMatrix()
		GlStateManager.disableTexture2D()
		GlStateManager.enableBlend()
		Rendering.blendSrcAlpha()
		GlStateManager.disableLighting()
		GL11.glLineWidth(1.5F)
		GL11.glBegin(GL11.GL_LINES)
	}

	def endLine(): Unit = {
		GL11.glEnd()
		GlStateManager.enableLighting()
		GlStateManager.disableBlend()
		GlStateManager.enableTexture2D()
		GlStateManager.popMatrix()
	}

	def calculateYForHeight(world: World, posY: Double, height: Double): (Double, Double) = {
		var yStart = Math.floor(posY - height / 2)
		var yEnd = yStart + height
		if (yStart < 0) {
			yStart = 0
			yEnd = height
		}
		if (yStart > world.getHeight) {
			yEnd = world.getHeight
			yStart = yEnd - height
		}
		(yStart, yEnd)
	}

	def renderChunkCornerWithColor(world: World, chunkX: Int, chunkZ: Int, posY: Double,
			red: Float, green: Float, blue: Float): Unit = {
		startLine()
		for {cx <- -4 to 4
		     cz <- -4 to 4} {
			val xStart = (chunkX + cx) << 4
			val zStart = (chunkZ + cz) << 4
			val xEnd = xStart + 16
			val zEnd = zStart + 16
			val y = this.calculateYForHeight(world, posY, 128)
			val yStart = y._1
			val yEnd = y._2

			val opacity = Math.pow(1.5, -(cx * cx + cz * cz)).toFloat
			GlStateManager.color(red, green, blue, opacity)
			if (cx >= 0 && cz >= 0) {
				GL11.glVertex3d(xEnd, yStart, zEnd)
				GL11.glVertex3d(xEnd, yEnd, zEnd)
			}
			if (cx >= 0 && cz <= 0) {
				GL11.glVertex3d(xEnd, yStart, zStart)
				GL11.glVertex3d(xEnd, yEnd, zStart)
			}
			if (cx <= 0 && cz >= 0) {
				GL11.glVertex3d(xStart, yStart, zEnd)
				GL11.glVertex3d(xStart, yEnd, zEnd)
			}
			if (cx <= 0 && cz <= 0) {
				GL11.glVertex3d(xStart, yStart, zStart)
				GL11.glVertex3d(xStart, yEnd, zStart)
			}
		}
		endLine()
	}

	def renderChunkBoundaryWithColor(world: World, chunkX: Int, chunkZ: Int, posY: Double,
			red: Float, green: Float, blue: Float): Unit = {
		val xStart = chunkX << 4
		val zStart = chunkZ << 4
		val xEnd = xStart + 16
		val zEnd = zStart + 16
		val y = this.calculateYForHeight(world, posY, 32)
		val yStart = y._1
		val yEnd = y._2

		startLine()
		GlStateManager.color(red, green, blue, 1F)
		// Horizontal
		for (y <- yStart.toInt to yEnd.toInt) {
			// East
			GL11.glVertex3d(xEnd, y, zStart)
			GL11.glVertex3d(xEnd, y, zEnd)
			// West
			GL11.glVertex3d(xStart, y, zStart)
			GL11.glVertex3d(xStart, y, zEnd)
			// North
			GL11.glVertex3d(xStart, y, zEnd)
			GL11.glVertex3d(xEnd, y, zEnd)
			// South
			GL11.glVertex3d(xStart, y, zStart)
			GL11.glVertex3d(xEnd, y, zStart)
		}
		// Vertical
		for (h <- 1 to 15) {
			// North
			GL11.glVertex3d(xStart + h, yStart, zStart)
			GL11.glVertex3d(xStart + h, yEnd, zStart)
			// South
			GL11.glVertex3d(xStart + h, yStart, zEnd)
			GL11.glVertex3d(xStart + h, yEnd, zEnd)
			// West
			GL11.glVertex3d(xStart, yStart, zStart + h)
			GL11.glVertex3d(xStart, yEnd, zStart + h)
			// East
			GL11.glVertex3d(xEnd, yStart, zStart + h)
			GL11.glVertex3d(xEnd, yEnd, zStart + h)
		}
		endLine()
	}


	def renderBounds(chunk: ChunkCoordIntPair, player: EntityPlayer): Unit = {
		GlStateManager.pushMatrix()
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
		Rendering.bindResource(ChunkCommander.getResource("chunkFace"))

		val maxY = 512D
		val maxTex = maxY / 16D
		val tess = Tessellator.getInstance()
		val wr = tess.getWorldRenderer
		def start(): Unit = wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)
		def end(): Unit = tess.draw()

		var color = Color.RED
		def colorEnd(): Unit =
			wr.color(color.getRed / 255F, color.getBlue / 255F, color.getGreen / 255F,
				0.5F).endVertex()

		val xStart = chunk.chunkXPos << 4
		val zStart = chunk.chunkZPos << 4
		GlStateManager.translate(xStart, 0, zStart)

		for (face <- EnumFacing.HORIZONTALS) {
			val shouldRenderFace = true
			val shouldRenderOuter = !this.isForceLoaded(chunk, face)
			val shouldRenderInner = shouldRenderOuter

			if (shouldRenderFace) {

				var xBounds = (0, 0)
				var zBounds = (0, 0)
				face match {
					case EnumFacing.NORTH => xBounds = (0, 16)
					case EnumFacing.SOUTH =>
						xBounds = (16, 0)
						zBounds = (16, 16)
					case EnumFacing.EAST =>
						zBounds = (0, 16)
						xBounds = (16, 16)
					case EnumFacing.WEST => zBounds = (16, 0)
					case _ =>
				}

				if (shouldRenderOuter) {
					GlStateManager.pushMatrix()
					GlStateManager.enableBlend()
					Rendering.blendSrcAlpha()
					start()
					// min max
					wr.pos(xBounds._1, maxY, zBounds._1).tex(0D, maxTex)
					colorEnd()
					// max max
					wr.pos(xBounds._2, maxY, zBounds._2).tex(1D, maxTex)
					colorEnd()
					// max min
					wr.pos(xBounds._2, 0, zBounds._2).tex(1D, 0D)
					colorEnd()
					// min min
					wr.pos(xBounds._1, 0, zBounds._1).tex(0D, 0D)
					colorEnd()
					end()
					GlStateManager.disableBlend()
					GlStateManager.popMatrix()
				}

				color = Color.BLUE
				if (shouldRenderInner) {
					GlStateManager.pushMatrix()
					GlStateManager.enableBlend()
					Rendering.blendSrcAlpha()
					start()
					// min min
					wr.pos(xBounds._1, 0, zBounds._1).tex(0D, 0D)
					colorEnd()
					// max min
					wr.pos(xBounds._2, 0, zBounds._2).tex(1D, 0D)
					colorEnd()
					// max max
					wr.pos(xBounds._2, maxY, zBounds._2).tex(1D, maxTex)
					colorEnd()
					// min max
					wr.pos(xBounds._1, maxY, zBounds._1).tex(0D, maxTex)
					colorEnd()
					end()
					GlStateManager.disableBlend()
					GlStateManager.popMatrix()
				}
				color = Color.RED

			}
		}

		GlStateManager.popMatrix()
	}

	def isForceLoaded(chunk: ChunkCoordIntPair, facing: EnumFacing): Boolean = {
		this.isForceLoaded(chunk.chunkXPos, chunk.chunkZPos, facing)
	}

	def isForceLoaded(chunkX: Int, chunkZ: Int, facing: EnumFacing): Boolean = {
		this.forcedChunks contains new ChunkCoordIntPair(
			chunkX + facing.getDirectionVec.getX,
			chunkZ + facing.getDirectionVec.getZ)
	}

	def isPlayerInRow(chunk: ChunkCoordIntPair, directionToCheck: EnumFacing,
			p: EntityPlayer): Boolean = {
		var currentChunkX = chunk.chunkXPos
		var currentChunkZ = chunk.chunkZPos
		val axis = directionToCheck.getAxis
		val dir = directionToCheck.getAxisDirection
		do {
			val xStart = currentChunkX << 4
			val xEnd = xStart + 15
			val zStart = currentChunkZ << 4
			val zEnd = zStart + 15
			val isIn = xStart <= p.posX && p.posX <= xEnd && zStart <= p.posZ && p.posZ <= zEnd
			if (isIn) return true
			else {
				axis match {
					case EnumFacing.Axis.X => currentChunkX += dir.getOffset
					case EnumFacing.Axis.Z => currentChunkZ += dir.getOffset
					case _ =>
				}
			}
		} while (isForceLoaded(currentChunkX, currentChunkZ, directionToCheck))

		false
	}

}
