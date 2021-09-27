package me.gommeantilegit.minecraft.earth

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import me.gommeantilegit.minecraft.earth.rendering.BlockModelBakery
import me.gommeantilegit.minecraft.earth.utils.BlockPos
import me.gommeantilegit.minecraft.earth.world.ChunkPosition
import me.gommeantilegit.minecraft.earth.world.ChunkResponse
import me.gommeantilegit.minecraft.earth.world.World
import me.gommeantilegit.minecraft.earth.world.WorldDisplayer
import me.gommeantilegit.minecraft.earth.world.generation.SuperFlatChunkGenerator

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var arScene: Scene
    private lateinit var arSession: Session
    private lateinit var arFragment: ArFragment
    private lateinit var worldDisplayer: WorldDisplayer
    private lateinit var debugText: TextView
    private var placed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        context = this
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_ux)

        debugText = findViewById(R.id.debugText)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        arScene = arFragment.arSceneView.scene

        BlockModelBakery.init(this)

        val world = World()
        val worldGenerator = SuperFlatChunkGenerator()

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _: Plane, _: MotionEvent ->
            if (placed) {
                return@setOnTapArPlaneListener
            }
            arSession = arFragment.arSceneView.session!!
            worldDisplayer = WorldDisplayer(4, arFragment, arSession, hitResult, world, worldGenerator)
            placed = true
        }
        arScene.addOnUpdateListener { frameTime ->
            doTick(frameTime!!)
        }
    }

    private fun doTick(frameTime: FrameTime) {
        if (!placed) {
            return
        }
        val viewerPosition = worldDisplayer.getViewerPosition(arScene.camera.localPosition)

        val currentBlockPos = BlockPos.of(viewerPosition)
        val currentChunkPos = currentBlockPos.chunkPos

        setDebugInfo(viewerPosition, currentBlockPos, currentChunkPos)

        worldDisplayer.onViewerMoved(viewerPosition)
    }

    private fun setDebugInfo(viewerPos: Vector3, blockPos: BlockPos, chunkPosition: ChunkPosition) {
        debugText.text = "ViewerPos: (x=${"%.2f".format(viewerPos.x)}, y=${"%.2f".format(viewerPos.y)}, z=${"%.2f".format(viewerPos.z)})\nBlockPos: (x=${blockPos.x}, y=${blockPos.y}, z=${blockPos.z})\nChunkPosition: (x=${chunkPosition.xPosition}, y=${chunkPosition.yPosition}, z=${chunkPosition.zPosition})"
    }

    companion object {
        @SuppressLint("StaticFieldLeak") // Prevented
        lateinit var context: Context
        private val TAG = MainActivity::class.java.simpleName
        private const val MIN_OPENGL_VERSION = 3.0

        /**
         * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
         * on this device.
         *
         *
         * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
         *
         *
         * Finishes the activity if Sceneform can not run
         */
        @SuppressLint("ObsoleteSdkInt")
        fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
                Log.e(TAG, "Sceneform requires Android N or later")
                Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
                activity.finish()
                return false
            }
            val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .deviceConfigurationInfo
                    .glEsVersion
            if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
                Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
                Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                        .show()
                activity.finish()
                return false
            }
            return true
        }
    }
}
