package com.physicsgeek75.bongo

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JSlider

class BongoSettingsConfigurable(private val project: Project) : Configurable {

    private var root: JComponent? = null
    private lateinit var enableCheck: JCheckBox
    private lateinit var sizeSlider: JSlider

    override fun getDisplayName(): String = "Bongo Cat"

    override fun createComponent(): JComponent {
        if (root == null) {
            root = panel {
                group("Sticker") {
                    row {
                        enableCheck = checkBox("Enable sticker").component
                    }
                    row("Size") {
                        // min=24, max=256, minor tick 4, major tick 16
                        sizeSlider = slider(24, 256, 4, 16).applyToComponent {
                            paintTicks = true
                            paintLabels = true
                            snapToTicks = true
                        }.component
                    }
                }
            }
            reset()
        }
        return root!!
    }

    override fun isModified(): Boolean {
        val svc = project.service<BongoStickerService>()
        return enableCheck.isSelected != svc.isVisible() ||
                sizeSlider.value != svc.getSizeDip()
    }

    override fun apply() {
        val svc = project.service<BongoStickerService>()
        svc.applySize(sizeSlider.value)
        svc.setVisible(enableCheck.isSelected)
        if (svc.isVisible()) svc.ensureAttached()
    }

    override fun reset() {
        val svc = project.service<BongoStickerService>()
        enableCheck.isSelected = svc.isVisible()
        sizeSlider.value = svc.getSizeDip()
    }

    override fun disposeUIResources() {
        root = null
    }
}
