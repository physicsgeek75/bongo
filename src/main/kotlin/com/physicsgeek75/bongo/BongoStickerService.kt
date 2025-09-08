package com.physicsgeek75.bongo

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBLabel
import com.intellij.util.*
import com.intellij.util.ui.JBUI
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.*
import java.awt.event.ComponentListener
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.*
import kotlin.math.max
import kotlin.math.min
import javax.sound.sampled.*


private const val DEFAULT_SIZE_DIP = 128
private const val DEFAULT_MARGIN_DIP = 0
private const val ICON_DIP = DEFAULT_SIZE_DIP


class StickerState {
    var designId: String = "classic"
    var visible: Boolean = true
    var xDip: Int = -1
    var yDip: Int = -1
    var sizeDip: Int = DEFAULT_SIZE_DIP

}

@State(name = "BongoStickerState", storages = [Storage("bongoSticker.xml")])
@Service(Service.Level.PROJECT)
class BongoStickerService(private val project: Project)
    : PersistentStateComponent<StickerState>, Disposable {

    private var state = StickerState()

    private var layeredPane: JLayeredPane? = null
    private var lpResizeListener: ComponentListener? = null
    private var panel: JPanel? = null
    private var label: JBLabel? = null
    private var icon1: Icon? = null
    private var icon2: Icon? = null
    private var toggle = false

    private val connection = ApplicationManager.getApplication().messageBus.connect(this)

    private var idleIcon=loadScaledIcon("/icons/idleIcon.svg",state.sizeDip)

    private var clip: Clip? = null
    var soundEnabled: Boolean = true
    private var lastPlay = 0L //ns

    private var punchActive = false

    private data class IconSet (val i1: Icon, val i2: Icon, val idle: Icon)
    private val iconCache = mutableMapOf<Pair<String, Int>, IconSet>()


    init {
        connection.subscribe(BongoTopic.TOPIC, object : BongoTopic {
            override fun tapped() = onTap()
        })
    }

    // ---------- PersistentStateComponent ----------
    override fun getState(): StickerState = state
    override fun loadState(s: StickerState) { state = s }

    // ---------- Lifecycle ----------
    fun ensureAttached() {
        if (!state.visible || panel != null) return

        val app = ApplicationManager.getApplication()
        if (!app.isDispatchThread) { // why was that there lol
            // talking to myself is actually crazy
            app.invokeLater({ ensureAttached() }, ModalityState.any())
            return
        }


        val frame = WindowManager.getInstance().getFrame(project) as? JFrame ?: return
        if (frame.rootPane == null) {
            // schedule one more try shortly
            app.invokeLater({ ensureAttached() }, ModalityState.any())
            return
        }


        val lp = frame.rootPane.layeredPane ?: run {
            app.invokeLater({ ensureAttached() }, ModalityState.any())
            return
        }

        if (layeredPane != null && layeredPane !== lp) {
            lpResizeListener?.let { old -> layeredPane!!.removeComponentListener(old) }
            lpResizeListener = null
        }

        layeredPane = lp

        val resize = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                clampIntoBounds()
            }
        }

        lp.addComponentListener(resize)
        lpResizeListener = resize


        updateIconsAndRefresh()

        label = JBLabel(icon1).apply {
            horizontalAlignment = JBLabel.CENTER
            verticalAlignment = JBLabel.CENTER
            toolTipText = "Bongo Cat"
            preferredSize = JBDimensionDip(state.sizeDip, state.sizeDip)
            minimumSize   = preferredSize
            maximumSize   = preferredSize
            cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
        }

        // Transparent container we can position freely
        panel = object : JPanel(null) {
            override fun isOpaque() = false
        }.apply {
            val sizePx = JBUI.scale(state.sizeDip)
            setSize(sizePx, sizePx)
            add(label)
            label!!.setBounds(0, 0, sizePx, sizePx)
            cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
            addDragHandlers(this,label!! ,lp)
        }

        // Compute starting spot
        val w = panel!!.width.toInt()
        val h = panel!!.height
        val start = computeStartPoint(lp, w, h)
        panel!!.setLocation(start)

        // Add to a high layer so it floats above editor
        layeredPane!!.add(panel, JLayeredPane.POPUP_LAYER,0)
        layeredPane!!.revalidate()
        layeredPane!!.repaint()



        // Keep it inside bounds on window resize
        lp.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                clampIntoBounds()
            }
        })
    }

    fun setVisible(visible: Boolean) {
        if (state.visible == visible) {
            if (visible && panel == null) ensureAttached()
            return
        }
        state.visible = visible
        if (visible) ensureAttached() else detach()
    }

    fun isVisible(): Boolean = state.visible

    fun getSizeDip(): Int = state.sizeDip

    private fun detach() {
        val app = ApplicationManager.getApplication()
        if (!app.isDispatchThread) {
            app.invokeAndWait({ detach() }, ModalityState.any())
            return
        }

        val lp = layeredPane

        lpResizeListener?.let { listener -> lp?.removeComponentListener(listener) }
        lpResizeListener = null

        panel?.let { p ->
            lp?.remove(p)
            lp?.revalidate()
            lp?.repaint()
        }

        idleTimer?.stop()

        panel = null
        label = null
        icon1 = null
        icon2 = null
        layeredPane = null
        toggle = false
    }

    fun applySize(newDip: Int) {
        state.sizeDip = newDip.coerceIn(0, 512)

        invalidateCache(sizeDip = state.sizeDip)

        if (state.visible && panel == null) {
            ensureAttached()
        }

        val sizePx = JBUI.scale(state.sizeDip)

        label?.apply {
            preferredSize = JBDimensionDip(state.sizeDip, state.sizeDip)
            minimumSize   = preferredSize
            maximumSize   = preferredSize
            setBounds(0, 0, sizePx.toInt(), sizePx)
            icon = if (toggle) icon2 else icon1
            revalidate()
            repaint()

            if(!toggle) icon=idleIcon

        }

        panel?.apply {
            setSize(sizePx, sizePx)
            revalidate()
            repaint()
        }

        layeredPane?.apply {
            revalidate()
            repaint()
        }

        updateIconsAndRefresh()

        clampIntoBounds()
    }

    fun resetPosition() {
        state.xDip = -1; state.yDip = -1
        layeredPane?.let { lp ->
            panel?.let { p ->
                val start = computeStartPoint(lp, p.width, p.height)
                p.setLocation(start)
                lp.revalidate(); lp.repaint()
            }
        }
    }

    fun resetSize() {
        applySize(DEFAULT_SIZE_DIP)
    }


    override fun dispose() {
        val app = ApplicationManager.getApplication()
        if (!app.isDispatchThread) {
            app.invokeAndWait({ detach() }, ModalityState.any())
        } else {
            detach()
        }

        runCatching {
            clip?.takeIf { it.isOpen }?.close()
        }
        clip = null

        // clips.forEach { runCatching { it.close() } }
        // clips = emptyArray()
    }

    private fun updateIconsAndRefresh() {
        val set = loadIconSet(state.designId, state.sizeDip)
        icon1 = set.i1
        icon2 = set.i2
        idleIcon = set.idle

        label?.apply {
            icon = when {
                toggle -> icon2
                idleTimer?.isRunning == false -> idleIcon
                else -> icon1
            }
            revalidate(); repaint()
        }
        panel?.revalidate(); panel?.repaint()
        layeredPane?.revalidate(); layeredPane?.repaint()
    }

    fun applyDesign(newId: String) {
        if (state.designId == newId) return
        state.designId = newId
        invalidateCache(designId = newId)
        if (state.visible && panel == null) ensureAttached()
        updateIconsAndRefresh()
    }




    // ---------- Behavior ----------
    private var idleTimer = Timer(2000) {
        toggle = false
        label?.icon = idleIcon
    }.apply { isRepeats = false }

    private fun onTap() {
        val p = panel ?: return
        val lbl = label ?: return
        val i1 = icon1 ?: return
        val i2 = icon2 ?: return

        idleTimer.restart();

        toggle = !toggle
        ApplicationManager.getApplication().invokeLater {
            lbl.icon = if (toggle) i2 else i1
            if (!punchActive) { punch(p) }
           // tiny “bounce” effect
            // At infrequent random times, the bounce effect will send the sticker down approx 1px. cause unknown but not noticeable or disruptive; will leave as it is for now.
            // Disregard the above comment, bug fixed as of V1.2.7
        }
    }

    private fun punch(p: JPanel) {
        if(punchActive) return

        val y0 = p.y
        val dy = JBUI.scale(2)
        punchActive=true
        p.setLocation(p.x, y0 + dy)
        Timer(90) {
            p.setLocation(p.x, y0)
            punchActive = false
        }.apply { isRepeats = false; start() }
    }



    // ---------- Helpers ----------
    private fun loadIconSet(designId: String, sizeDip: Int): IconSet {
        val key = designId to sizeDip
        return iconCache.getOrPut(key) {
            fun L(name: String) = loadScaledIcon("/designs/$designId/$name.svg", sizeDip)
            IconSet(L("icon1"), L("icon2"), L("idleIcon"))
        }
    }

    private fun invalidateCache(designId: String? = null, sizeDip: Int? = null) {
        if (designId == null && sizeDip == null) iconCache.clear()
        else iconCache.keys.removeIf { (d, s) ->
            (designId == null || d == designId) && (sizeDip == null || s == sizeDip)
        }
    }


    fun preloadClip() {
        if(clip!=null && clip!!.isOpen) return

        val url = javaClass.getResource("/sounds/click.wav")
            ?: return

        AudioSystem.getAudioInputStream(url).use { original ->
            val base = original.format
            val target =
                if (base.encoding == AudioFormat.Encoding.PCM_SIGNED) base
                else AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.sampleRate,
                    16,
                    base.channels,
                    base.channels * 2,
                    base.sampleRate,
                    false
                )

            val toLoad = if (target == base) original
            else AudioSystem.getAudioInputStream(target, original)

            val c = AudioSystem.getClip()
            c.open(toLoad)
            clip = c
        }


    }

    fun playClick() {
        if (!soundEnabled) return
        val now = System.nanoTime()
        if (now - lastPlay < 20_000_000L) return  // 20 ms cooldown
        lastPlay = now

        val c = clip ?: run { preloadClip(); clip } ?: return
        if (c.isRunning) c.stop()
        c.framePosition = 0
        c.start()
    }

    private fun loadScaledIcon(path: String, dip: Int): Icon {
        val raw = try { IconLoader.getIcon(path, javaClass) }
        catch (_: Throwable) { com.intellij.icons.AllIcons.General.Information }
        val targetPx = JBUI.scale(dip)
        val baseH = max(1, raw.iconHeight)
        val scale = targetPx.toFloat() / baseH
        return IconUtil.scale(raw, null, scale)
    }

    private fun JBDimensionDip(wDip: Int, hDip: Int) =
        com.intellij.util.ui.JBDimension(JBUI.scale(wDip), JBUI.scale(hDip))

    private fun computeStartPoint(lp: JLayeredPane, w: Int, h: Int): Point {
        val margin = JBUI.scale(DEFAULT_MARGIN_DIP)
        val x = if (state.xDip >= 0) JBUI.scale(state.xDip) else lp.width - w - margin
        val y = if (state.yDip >= 0) JBUI.scale(state.yDip) else lp.height - h - margin
        return Point(x.coerceIn(0, max(0, lp.width - w)),
            y.coerceIn(0, max(0, lp.height - h)))
    }

    private fun clampIntoBounds() {
        val lp = layeredPane ?: return
        val p = panel ?: return
        val maxX = max(0, lp.width - p.width)
        val maxY = max(0, lp.height - p.height)
        val nx = p.x.coerceIn(0, maxX)
        val ny = p.y.coerceIn(0, maxY)
        if (nx != p.x || ny != p.y) p.setLocation(nx, ny)
    }

    private fun addDragHandlers(panel: JPanel, label: JComponent, lp: JLayeredPane) {
        val ma = object : java.awt.event.MouseAdapter() {
            private var dx = 0
            private var dy = 0
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                val pt = SwingUtilities.convertPoint(e.component, e.point, lp)
                dx = pt.x - panel.x
                dy = pt.y - panel.y
            }
            override fun mouseDragged(e: java.awt.event.MouseEvent) {
                val pt = SwingUtilities.convertPoint(e.component, e.point, lp)
                val nx = (pt.x - dx).coerceIn(0, kotlin.math.max(0, lp.width - panel.width))
                val ny = (pt.y - dy).coerceIn(0, kotlin.math.max(0, lp.height - panel.height))
                panel.setLocation(nx, ny)
                // persist as DIP
                state.xDip = JBUI.unscale(nx)
                state.yDip = JBUI.unscale(ny)
            }
        }
        panel.addMouseListener(ma); panel.addMouseMotionListener(ma)
        label.addMouseListener(ma); label.addMouseMotionListener(ma)
    }

    private fun Int.coerceIn(minVal: Int, maxVal: Int) = min(max(this, minVal), maxVal)
}
