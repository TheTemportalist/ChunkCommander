package temportalist.chunkcommander.main.client

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.opengl.GL11
import temportalist.origin.api.client.Rendering
import temportalist.origin.api.common.lib.Vect

import scala.collection.mutable.ListBuffer

/**
  *
  * Created by TheTemportalist on 1/15/2016.
  *
  * @author TheTemportalist
  */
@SideOnly(Side.CLIENT)
object WorldRender {

	private var showChunkBoundariesState = 0

	def toggleChunkBoundariesAndCorners(): Unit = {
		showChunkBoundariesState += 1
		if (showChunkBoundariesState >= 4) this.showChunkBoundariesState = 0
	}

	private val forcedChunks = ListBuffer[ChunkPos]()

	def setForcedChunks(set: Array[ChunkPos]): Unit = {
		this.forcedChunks.clear()
		if (set != null)
			this.forcedChunks ++= set
	}

	def addForcedChunk(chunk: ChunkPos): Unit = {
		this.forcedChunks += chunk
	}

	def removeForcedChunk(chunk: ChunkPos): Unit = {
		this.forcedChunks -= chunk
	}

	def isChunkLoaded(world: World, chunk: ChunkPos): Boolean = {
		world.getChunkProvider.getLoadedChunk(chunk.chunkXPos, chunk.chunkZPos) != null &&
				!world.getChunkProvider.provideChunk(chunk.chunkXPos, chunk.chunkZPos).isEmpty
	}

	@SubscribeEvent
	def worldRenderLast(event: RenderWorldLastEvent): Unit = {
		val p = Minecraft.getMinecraft.thePlayer
		val world = Minecraft.getMinecraft.theWorld
		val partialTick = event.getPartialTicks

		GlStateManager.pushMatrix()
		GlStateManager.translate(
			-(p.lastTickPosX + (p.posX - p.lastTickPosX) * partialTick),
			-(p.lastTickPosY + (p.posY - p.lastTickPosY) * partialTick),
			-(p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * partialTick)
		)

		// Render chunk corners
		if (this.showChunkBoundariesState > 0) {
			GlStateManager.pushMatrix()
			this.renderChunkCornerWithColor(world, p.chunkCoordX, p.chunkCoordZ,
				p.posY, 0.9f, 0F, 0F)
			this.renderLineCenter(world, p.chunkCoordX, p.chunkCoordZ, p.posY,
				0.9F, 0.9F, 0F, opacity = 0.9F)
			GlStateManager.popMatrix()
		}

		// Render current chunk boundaries
		if (this.showChunkBoundariesState == 2) {
			GlStateManager.pushMatrix()
			this.renderChunkBoundaryWithColor(p.chunkCoordX, p.chunkCoordZ,
				(face: EnumFacing) => true,
				this.calculateYForHeight(world, p.posY, 32), 0F, 0.9F, 0F)
			GlStateManager.popMatrix()
		}

		// Render loaded chunk boundaries
		if (this.showChunkBoundariesState == 3) {
			GlStateManager.pushMatrix()
			for (chunk <- this.forcedChunks) if (isChunkLoaded(world, chunk)) {
				this.renderChunkBoundaryWithColor(chunk.chunkXPos, chunk.chunkZPos,
					(face: EnumFacing) => !this.isForceLoaded(chunk, face),
					this.calculateYForHeight(world, p.posY, 32), 0F, 0F, 0.9F)

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

	def renderLineCenter(world: World, chunkX: Int, chunkZ: Int, posY: Double,
			red: Float, green: Float, blue: Float, opacity: Float = 1F): Unit = {

		val y = this.calculateYForHeight(world, posY, 128)
		val yS = y._1 // start
		val yE = y._2 // end

		startLine()
		GlStateManager.color(red, green, blue, opacity)
		for {
			cX <- chunkX - 1 to chunkX + 1
			cZ <- chunkZ - 1 to chunkZ + 1
		} {
			GL11.glVertex3d((cX << 4) + 8, yS, (cZ << 4) + 8)
			GL11.glVertex3d((cX << 4) + 8, yE, (cZ << 4) + 8)
		}
		endLine()
	}

	def renderChunkBoundaryWithColor(chunkX: Int, chunkZ: Int, doRender: (EnumFacing) => Boolean,
			yStartEnd: (Double, Double), red: Float, green: Float, blue: Float, opacity: Float = 1F): Unit = {
		val start = new Vect(chunkX << 4, yStartEnd._1, chunkZ << 4)
		val end = new Vect(start.x + 16, yStartEnd._2, start.z + 16)

		startLine()
		GlStateManager.color(red, green, blue, opacity)
		for (face <- EnumFacing.HORIZONTALS) {
			for {yOffset <- 0 to end.y_i() - start.y_i()
			     horizontalOffset <- 1 to 15} {
				if (doRender(face)) {
					this.addLineForChunkFace(
						face, horizontalOrVertical = true, start, end, yOffset)
					this.addLineForChunkFace(
						face, horizontalOrVertical = false, start, end, horizontalOffset)
				}
			}
		}
		endLine()
	}

	def addLineForChunkFace(face: EnumFacing, horizontalOrVertical: Boolean,
			start: Vect, end: Vect, offset: Int): Unit = {
		face match {
			case EnumFacing.NORTH => // -Z
				if (horizontalOrVertical) {
					GL11.glVertex3d(start.x, start.y + offset, start.z)
					GL11.glVertex3d(end.x, start.y + offset, start.z)
				}
				else {
					GL11.glVertex3d(start.x + offset, start.y, start.z)
					GL11.glVertex3d(start.x + offset, end.y, start.z)
				}
			case EnumFacing.SOUTH => // +Z
				if (horizontalOrVertical) {
					GL11.glVertex3d(start.x, start.y + offset, end.z)
					GL11.glVertex3d(end.x, start.y + offset, end.z)
				}
				else {
					GL11.glVertex3d(start.x + offset, start.y, end.z)
					GL11.glVertex3d(start.x + offset, end.y, end.z)
				}
			case EnumFacing.WEST => // -X
				if (horizontalOrVertical) {
					GL11.glVertex3d(start.x, start.y + offset, start.z)
					GL11.glVertex3d(start.x, start.y + offset, end.z)
				}
				else {
					GL11.glVertex3d(start.x, start.y, start.z + offset)
					GL11.glVertex3d(start.x, end.y, start.z + offset)
				}
			case EnumFacing.EAST => // +X
				if (horizontalOrVertical) {
					GL11.glVertex3d(end.x, start.y + offset, start.z)
					GL11.glVertex3d(end.x, start.y + offset, end.z)
				}
				else {
					GL11.glVertex3d(end.x, start.y, start.z + offset)
					GL11.glVertex3d(end.x, end.y, start.z + offset)
				}
			case _ =>
		}
	}

	def isForceLoaded(chunk: ChunkPos, facing: EnumFacing): Boolean = {
		this.isForceLoaded(chunk.chunkXPos, chunk.chunkZPos, facing)
	}

	def isForceLoaded(chunkX: Int, chunkZ: Int, facing: EnumFacing): Boolean = {
		this.forcedChunks contains new ChunkPos(
			chunkX + facing.getDirectionVec.getX,
			chunkZ + facing.getDirectionVec.getZ)
	}

	def isPlayerInRow(chunk: ChunkPos, directionToCheck: EnumFacing,
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
