package com.physicsgeek75.bongo

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.application.ApplicationManager

class TypedHandler(private val original: TypedActionHandler): TypedActionHandler {
    override fun execute(editor: Editor, c: Char, dc: DataContext) {
        original.execute(editor,c,dc)

        ApplicationManager.getApplication().messageBus.syncPublisher(BongoTopic.TOPIC).tapped()

    }

}