package com.dim.gen

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import java.awt.event.ActionEvent
import java.io.File

class GenerateDimensAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        println("actionPerformed was called!")
        val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)
        println("File: $file")
        if (file != null) {
            println("File is not null")
        }

        if (file != null) {
            val dialog = DimensDialog(e.project!!, e)
            dialog.isOKActionEnabled = false
            if (dialog.showAndGet()) {
                // ... ваш код ...
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)
        val isAvailable = file != null && file.extension == "xml"
        e.presentation.isEnabledAndVisible = isAvailable
    }
}
