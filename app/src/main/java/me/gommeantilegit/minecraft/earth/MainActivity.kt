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
import android.widget.Toast
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import me.gommeantilegit.minecraft.earth.block.Blocks
import me.gommeantilegit.minecraft.earth.rendering.BlockModelBakery
import me.gommeantilegit.minecraft.earth.world.BlockState
import me.gommeantilegit.minecraft.earth.world.World

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var placed = false

    /**
     * Handler on UI thread
     */
    private val handler = Handler(Looper.getMainLooper()) { message ->
        val obj = message.obj
        if (obj is World.ChunkResponse) {
            val andy = TransformableNode(arFragment.transformationSystem)
            andy.setParent(obj.anchorNode)
            val definition = RenderableDefinition.builder().setVertices(obj.vertices).setSubmeshes(obj.meshes).build()
            ModelRenderable.builder().setSource(definition).build().thenAccept { renderable ->
                andy.renderable = renderable
                andy.select()
            }
        }
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        context = this
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_ux)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment


        BlockModelBakery.init(this)

        val chunk = World.Chunk()

        // Test blocks

        for (x in 0 until 16) {
            for (z in 0 until 16) {
                chunk.setBlock(x, 0, z, BlockState(Blocks.bedrock))
                chunk.setBlock(x, 1, z, BlockState(Blocks.stone))
                chunk.setBlock(x, 2, z, BlockState(Blocks.dirt))
                chunk.setBlock(x, 3, z, BlockState(Blocks.grass))
            }
        }

        chunk.setBlock(8, 4, 8, BlockState(Blocks.stone))
        chunk.setBlock(8, 5, 8, BlockState(Blocks.stone))
        chunk.setBlock(8, 6, 8, BlockState(Blocks.stone))
        chunk.setBlock(8, 6, 7, BlockState(Blocks.stone))
        chunk.setBlock(8, 6, 6, BlockState(Blocks.stone))
        chunk.setBlock(8, 5, 6, BlockState(Blocks.stone))
        chunk.setBlock(8, 4, 6, BlockState(Blocks.stone))

        chunk.setBlock(6, 6, 6, BlockState(Blocks.bedrock))
        chunk.setBlock(6, 6, 4, BlockState(Blocks.grass))
        chunk.setBlock(6, 5, 4, BlockState(Blocks.dirt))
        chunk.setBlock(6, 4, 4, BlockState(Blocks.stone))


        for (x in 1..3) {
            for (y in 1..3) {
                for (z in 1..3) {
                    chunk.setBlock(x, y, z, BlockState(Blocks.air))
                }
            }
        }

        chunk.setBlock(3, 1, 3, BlockState(Blocks.diamondOre))

        val tree =
                arrayOf(

                        ("     \n" +
                                "     \n" +
                                "  x  \n" +
                                "     \n" +
                                "     \n"),

                        ("     \n" +
                                "     \n" +
                                "  x  \n" +
                                "     \n" +
                                "     \n"),


                        ("     \n" +
                                "     \n" +
                                "  x  \n" +
                                "     \n" +
                                "     \n"),

                        (".....\n" +
                                ".....\n" +
                                "..x..\n" +
                                ".....\n" +
                                ".... \n"),

                        (" ... \n" +
                                ".....\n" +
                                "..x..\n" +
                                ".....\n" +
                                " ... \n"),

                        ("     \n" +
                                " ... \n" +
                                " .x. \n" +
                                " ... \n" +
                                "     \n"),

                        ("     \n" +
                                "  .  \n" +
                                " ... \n" +
                                "  .  \n" +
                                "     \n")
                )

        val x = 2
        val z = 2
        val y = 4

        for ((yo, f) in tree.withIndex()) {
            for ((xo, line) in f.lines().withIndex()) {
                for ((zo, c) in line.toCharArray().withIndex()) {
                    chunk.setBlock(x + xo, y + yo, z + zo, BlockState(when (c) {
                        ' ' -> Blocks.air
                        '.' -> Blocks.leaves
                        'x' -> Blocks.log
                        else -> Blocks.air
                    }))
                }
            }
        }

        val groundY = 4f

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _: Plane, _: MotionEvent ->
            if (placed) {
                return@setOnTapArPlaneListener
            }

            val anchor = hitResult.trackable.createAnchor(hitResult.hitPose.compose(Pose.makeTranslation(0f, -groundY, 0f)))

            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            chunk.createRenderable(handler, anchorNode)
            placed = true
        }
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
