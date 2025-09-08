package com.physicsgeek75.bongo

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.panel
import java.awt.Component
import javax.swing.*



class BongoSettingsConfigurable(private val project: Project) : Configurable {

    private var root: JComponent? = null
    private lateinit var enableCheck: JCheckBox
    private lateinit var sizeSlider: JSlider
    private lateinit var sizeLabel: JLabel
    private lateinit var designCombo: JComboBox<Design>
    private val svc = project.service<BongoStickerService>()
    override fun getDisplayName(): String = "Bongo Cat"

    override fun createComponent(): JComponent {

        val svc = project.service<BongoStickerService>()
        val designs = BongoDesigns.list(javaClass)

        designCombo = ComboBox(DefaultComboBoxModel(designs.toTypedArray())).apply {
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
                ): Component = super.getListCellRendererComponent(
                    list, (value as? Design)?.name ?: value, index, isSelected, cellHasFocus
                )
            }
        }

        if (root == null) {
            root = panel {
                group("Main", ) {
                    row {
                        enableCheck = checkBox("Enable").component
                    }

                    row {
                        sizeLabel = label("Size: ${svc.getSizeDip()} DIP").component

                        // min 0 max 512
                        sizeSlider = slider(0, 512, 0, majorTickSpacing = 0).applyToComponent {
                            paintTicks = false
                            paintLabels = false
                            snapToTicks = false
                            addChangeListener { sizeLabel.text = "Size: $value DIP" }
                        }.component
                    }

                    row {
                        button("Reset") {
                            project.service<BongoStickerService>().resetPosition()
                            project.service<BongoStickerService>().resetSize()
                            reset()
                        }
                    }
                    row("Bongo Design:") {
                        cell(designCombo)
                    }
                }
            }
            reset()
        }
        return root!!
    }

    override fun isModified(): Boolean {
        val svc = project.service<BongoStickerService>()
        val sel = (designCombo.selectedItem as Design).id
        return sel != svc.state.designId ||
                enableCheck.isSelected != svc.isVisible() ||
                sizeSlider.value != svc.getSizeDip()
    }

    override fun apply() {
        val svc = project.service<BongoStickerService>()
        val sel = (designCombo.selectedItem as Design).id
        svc.applyDesign(sel)
        svc.applySize(sizeSlider.value)
        svc.setVisible(enableCheck.isSelected)
        if (svc.isVisible()) svc.ensureAttached()

        sizeLabel.text = "Size: ${svc.getSizeDip()} DIP"
    }

    override fun reset() {
        val svc = project.service<BongoStickerService>()
        val designs = (0 until designCombo.model.size).map { designCombo.model.getElementAt(it) as Design }
        val idx = designs.indexOfFirst { it.id == svc.state.designId }.takeIf { it >= 0 } ?: 0
        designCombo.selectedIndex = idx

        enableCheck.isSelected = svc.isVisible()
        sizeSlider.value = svc.getSizeDip()

        sizeLabel.text = "Size: ${svc.getSizeDip()} DIP"

    }

    override fun disposeUIResources() {
        root = null
    }
}
