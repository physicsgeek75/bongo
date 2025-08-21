package com.physicsgeek75.bongo

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.startup.StartupActivity

class Startup : StartupActivity, DumbAware {
    override fun runActivity(project: Project) {
        // handler installation app level once
        ApplicationManager.getApplication().service<BongoTypingService>()
        project.service<BongoStickerService>().ensureAttached()
    }
}