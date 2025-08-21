package com.physicsgeek75.bongo

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.actionSystem.TypedActionHandler

@Service(Service.Level.APP)
class BongoTypingService : Disposable {

    private val typed: TypedAction = TypedAction.getInstance()

    private val previous: TypedActionHandler = typed.rawHandler

    init {
        typed.setupRawHandler(TypedHandler(previous))
    }

    override fun dispose() {
        val current = typed.rawHandler
        if (current is TypedHandler) {
            typed.setupRawHandler(previous)
        }
    }
}
