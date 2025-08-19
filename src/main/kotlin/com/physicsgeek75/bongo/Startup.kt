package com.physicsgeek75.bongo

import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class Startup : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        val manager = EditorActionManager.getInstance()
        val typed = manager.typedAction
        val original = typed.rawHandler
        typed.setupRawHandler(TypedHandler(original))


}
}