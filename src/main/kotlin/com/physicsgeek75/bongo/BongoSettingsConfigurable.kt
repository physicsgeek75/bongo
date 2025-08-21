package com.physicsgeek75.bongo

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JSlider

class BongoSettingsConfigurable(private val project: Project) : Configurable {

    private var root: JComponent? = null
    private lateinit var enableCheck: JCheckBox
    private lateinit var sizeSlider: JSlider
    private lateinit var sizeLabel: JLabel
    private val svc = project.service<BongoStickerService>()
    override fun getDisplayName(): String = "Bongo Cat"

    override fun createComponent(): JComponent {
        if (root == null) {
            root = panel {
                group("Main", ) {
                    row {
                        enableCheck = checkBox("Enable").component
                    }

                    row {
                        sizeLabel = label("Size: ${svc.getSizeDip()} px").component

                        // min 0 max 512
                        sizeSlider = slider(0, 512, 0, majorTickSpacing = 0).applyToComponent {
                            paintTicks = false
                            paintLabels = false
                            snapToTicks = false
                            addChangeListener { sizeLabel.text = "Size: $value px" }
                        }.component
                    }

                    row {
                        button("Reset") {
                            project.service<BongoStickerService>().resetPosition()
                            project.service<BongoStickerService>().resetSize()
                            reset()
                        }
                    }
                }
            }
            reset()
        }
        return root!!
    }

    override fun isModified(): Boolean {
        return enableCheck.isSelected != svc.isVisible() ||
                sizeSlider.value != svc.getSizeDip()
    }

    override fun apply() {
        val svc = project.service<BongoStickerService>()
        svc.applySize(sizeSlider.value)
        svc.setVisible(enableCheck.isSelected)
        if (svc.isVisible()) svc.ensureAttached()

        sizeLabel.text = "Size: ${svc.getSizeDip()} px"
    }

    override fun reset() {
        val svc = project.service<BongoStickerService>()
        enableCheck.isSelected = svc.isVisible()
        sizeSlider.value = svc.getSizeDip()

        sizeLabel.text = "Size: ${svc.getSizeDip()} px"

    }

    override fun disposeUIResources() {
        root = null
    }
}
