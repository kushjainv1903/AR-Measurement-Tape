package com.lenovo.artapemeasure

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Trackable
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var tvDistance: TextView
    private lateinit var btnMeasure: Button
    private lateinit var btnReset: Button

    private var pointMaterial: Material? = null
    private var lineMaterial: Material? = null

    private var startNode: AnchorNode? = null
    private var endNode: AnchorNode? = null
    private var lineNode: Node? = null

    private var measuring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        arFragment.arSceneView.scene.addOnUpdateListener {
            val frame = arFragment.arSceneView.arFrame ?: return@addOnUpdateListener
            val camera = frame.camera
            if (camera.trackingState != com.google.ar.core.TrackingState.TRACKING) {
                return@addOnUpdateListener
            }
        }
        tvDistance = findViewById(R.id.tvDistance)
        btnMeasure = findViewById(R.id.btnMeasure)
        btnReset = findViewById(R.id.btnReset)

        setupMaterials()
        setupButtons()
        setupFrameUpdates()
        disableTapToPlaceFromArFragment()
    }

    private fun setupMaterials() {
        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.CYAN))
            .thenAccept { pointMaterial = it }

        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.YELLOW))
            .thenAccept { lineMaterial = it }
    }

    private fun setupButtons() {
        btnMeasure.setOnClickListener {
            if (!measuring) {
                startMeasurement()
            } else {
                stopMeasurement()
            }
        }

        btnReset.setOnClickListener {
            resetMeasurement()
        }
    }

    private fun setupFrameUpdates() {
        arFragment.arSceneView.scene.addOnUpdateListener {
            if (!measuring) return@addOnUpdateListener
            val pose = getPoseFromScreenCenter() ?: return@addOnUpdateListener
            updateEndPointAndLine(pose)
        }
    }

    private fun disableTapToPlaceFromArFragment() {
        arFragment.setOnTapArPlaneListener { _, _, _ ->
            // We use center reticle + Start/Stop flow, so no tap-to-place behavior.
        }
    }

    private fun startMeasurement() {
        if (pointMaterial == null || lineMaterial == null) {
            Toast.makeText(this, "Materials not ready yet. Try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val pose = getPoseFromScreenCenter()
        if (pose == null) {
            Toast.makeText(this, "Point to a detected surface first.", Toast.LENGTH_SHORT).show()
            return
        }

        resetMeasurement()
        createStartPoint(pose)
        createOrUpdateEndPoint(pose)
        updateLine()

        measuring = true
        btnMeasure.text = getString(R.string.stop_measurement)
    }

    private fun stopMeasurement() {
        measuring = false
        btnMeasure.text = getString(R.string.start_measurement)
    }

    private fun resetMeasurement() {
        measuring = false
        btnMeasure.text = getString(R.string.start_measurement)

        lineNode?.setParent(null)
        lineNode = null

        startNode?.anchor?.detach()
        startNode?.setParent(null)
        startNode = null

        endNode?.anchor?.detach()
        endNode?.setParent(null)
        endNode = null

        tvDistance.text = getString(R.string.distance_default)
    }

    private fun getPoseFromScreenCenter(): Pose? {
        val frame = arFragment.arSceneView.arFrame ?: return null
        val cx = arFragment.arSceneView.width / 2f
        val cy = arFragment.arSceneView.height / 2f

        val hitResults = frame.hitTest(cx, cy)
        val validHit = hitResults.firstOrNull { isSurfaceHit(it) } ?: return null
        return validHit.hitPose
    }

    private fun isSurfaceHit(hit: HitResult): Boolean {
        val trackable: Trackable = hit.trackable
        return when (trackable) {
            is Plane -> trackable.isPoseInPolygon(hit.hitPose)
            is Point -> trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
            else -> false
        }
    }

    private fun createStartPoint(pose: Pose) {
        val session = arFragment.arSceneView.session ?: return
        val anchor = session.createAnchor(pose)
        val node = AnchorNode(anchor)
        node.setParent(arFragment.arSceneView.scene)
        node.renderable = createSphereRenderable(0.01f, pointMaterial!!)
        startNode = node
    }

    private fun createOrUpdateEndPoint(pose: Pose) {
        val existingNode = endNode
        if (existingNode == null) {
            val session = arFragment.arSceneView.session ?: return
            val anchor = session.createAnchor(pose)
            val node = AnchorNode(anchor)
            node.setParent(arFragment.arSceneView.scene)
            node.renderable = createSphereRenderable(0.01f, pointMaterial!!)
            endNode = node
            return
        }

        val oldAnchor = existingNode.anchor
        val session = arFragment.arSceneView.session ?: return
        val newAnchor = session.createAnchor(pose)
        existingNode.anchor = newAnchor
        oldAnchor?.detach()
    }

    private fun updateEndPointAndLine(pose: Pose) {
        createOrUpdateEndPoint(pose)
        updateLine()
    }

    private fun updateLine() {
        val s = startNode?.worldPosition ?: return
        val e = endNode?.worldPosition ?: return

        val diff = Vector3.subtract(e, s)
        val lengthMeters = diff.length()
        if (abs(lengthMeters) < 1e-4) return

        val center = Vector3.add(s, e).scaled(0.5f)
        val direction = diff.normalized()
        val rotation = Quaternion.lookRotation(direction, Vector3.up())

        val cube = ShapeFactory.makeCube(
            Vector3(0.005f, 0.005f, lengthMeters),
            Vector3.zero(),
            lineMaterial,
        )

        val node = lineNode ?: Node().also {
            it.setParent(arFragment.arSceneView.scene)
            lineNode = it
        }
        node.renderable = cube
        node.worldPosition = center
        node.worldRotation = rotation

        val cm = lengthMeters * 100f
        val inches = cm / 2.54f
        tvDistance.text = "Distance: %.2f cm | %.2f m | %.2f in".format(cm, lengthMeters, inches)
    }

    private fun createSphereRenderable(radius: Float, material: Material): ModelRenderable {
        return ShapeFactory.makeSphere(radius, Vector3.zero(), material)
    }

    override fun onPause() {
        super.onPause()
        stopMeasurement()
    }

    override fun onDestroy() {
        super.onDestroy()
        resetMeasurement()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Keep default touch behavior. ArFragment still handles camera gesture naturally.
        return super.onTouchEvent(event)
    }
}
