package com.dim.gen

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.awt.event.ActionEvent
import java.io.*
import javax.xml.stream.XMLOutputFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.roundToInt


class DimensDialog(private val project: Project, private val actionEvent: AnActionEvent) : DialogWrapper(project) {
    private val attributes = mapOf(
        "minWidth" to "min_width",
        "minHeight" to "min_height",
        "layout_width" to "layout_width",
        "layout_height" to "layout_height",
        "layout_marginStart" to "layout_margin_start",
        "layout_marginTop" to "layout_margin_top",
        "layout_marginEnd" to "layout_margin_end",
        "layout_marginBottom" to "layout_margin_bottom",
        "paddingStart" to "padding_start",
        "paddingEnd" to "padding_end",
        "paddingTop" to "padding_top",
        "paddingBottom" to "padding_bottom",
        "paddingVertical" to "padding_vertical",
        "paddingHorizontal" to "padding_horizontal",
        "padding" to "padding"
    )

    private val mainPanel = JPanel(BorderLayout())
    private val optionsPanel = JPanel(GridLayout(0, 2))
    private val widthMultipliers = mutableMapOf<String, JBTextField>(
        "sw400dp" to JBTextField("1.1"),
        "sw600dp" to JBTextField("1.25"),
        "sw720dp" to JBTextField("1.4"),
        "sw800dp" to JBTextField("1.5")
    )
    private val optionalCheckboxes = mutableMapOf<String, JBCheckBox>()
    private val backupCheckbox = JBCheckBox("Generate backup of the layout", true)

    private val logArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    private val logScrollPane = JScrollPane(
        logArea,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ).apply {
        preferredSize = java.awt.Dimension(400, 100)
    }

    init {
        title = "Generate Dimens"

        optionsPanel.add(JBLabel("Select dimensions and set multipliers:"))
        optionsPanel.add(JBLabel())

        widthMultipliers.forEach { (dpi, textField) ->
            val checkbox = JBCheckBox(dpi, true)
            optionalCheckboxes[dpi] = checkbox

            optionsPanel.add(checkbox)
            optionsPanel.add(textField)
        }

        optionsPanel.add(backupCheckbox)
        optionsPanel.add(JBLabel())

        mainPanel.add(optionsPanel, BorderLayout.NORTH)
        mainPanel.add(logScrollPane, BorderLayout.CENTER)

        init()
        log("DimensDialog started!")
    }

    override fun createActions(): Array<Action> {
        val applyAction = object : DialogWrapperAction("Generate") {
            override fun doAction(e: ActionEvent?) {
                applyChanges()
            }
        }

        return arrayOf(applyAction, cancelAction)
    }

    private fun applyChanges() {
        val virtualFile = actionEvent.getData(PlatformDataKeys.VIRTUAL_FILE)
        if (virtualFile != null) {
            val physicalFile = File(virtualFile.path)
            println("Going to process XML file: $physicalFile")
            processXmlFile(physicalFile)
            virtualFile.refresh(false, true)
            virtualFile.parent?.refresh(false, true)
            VirtualFileManager.getInstance().syncRefresh()
        }
    }


    override fun createCenterPanel(): JComponent {
        return mainPanel
    }

    fun getWidthMultiplier(dpi: String): Float {
        return widthMultipliers[dpi]?.text?.toFloatOrNull() ?: 1.0f
    }

    fun isDimensionEnabled(dpi: String): Boolean {
        return optionalCheckboxes[dpi]?.isSelected == true
    }

    fun isBackupEnabled(): Boolean {
        return backupCheckbox.isSelected
    }

    fun log(message: String) {
        SwingUtilities.invokeLater {
            logArea.append("$message\n")
            logArea.caretPosition = logArea.document.length
        }
    }

    fun calculateDimensValue(baseSize: Int, factor: Float): Int {
        return (baseSize * factor).roundToInt()
    }

    fun appendDimenToFile(path: String, dimenName: String, dimenValue: Int) {
        log("Appending/updating dimen in file: $path, $dimenName = ${dimenValue}dp")

        try {
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = documentBuilder.parse(path)

            val root = document.documentElement

            var existingDimen: Element? = null
            val dimens = root.getElementsByTagName("dimen")
            for (i in 0 until dimens.length) {
                val element = dimens.item(i) as Element
                if (element.getAttribute("name") == dimenName) {
                    existingDimen = element
                    break
                }
            }

            if (existingDimen != null) {
                log("Existing dimen found: $dimenName. Updating...")
                existingDimen.textContent = "${dimenValue}dp"
            } else {
                log("Creating new dimen: $dimenName")
                val newDimen = document.createElement("dimen")
                newDimen.setAttribute("name", dimenName)
                newDimen.appendChild(document.createTextNode("${dimenValue}dp"))

                val newLine = document.createTextNode("\n")

                // Добавляем новый элемент перед закрывающим тегом </resources>
                root.appendChild(newLine)
                root.appendChild(newDimen)
            }

            val transformer = TransformerFactory.newInstance().newTransformer()

            // Отключаем автоматическое форматирование, чтобы сохранить исходное форматирование
            transformer.setOutputProperty(OutputKeys.INDENT, "no")

            val writer = PrintWriter(FileOutputStream(path))
            val result = StreamResult(writer)
            transformer.transform(DOMSource(document), result)
        } catch (e: Exception) {
            log("Error appending/updating dimen: ${e.message}")
            e.printStackTrace()
        }
    }







    fun processXmlFile(selectedFile: File) {
        log("processXmlFile called!")
        log("Start processing file: ${selectedFile.absolutePath}")

        try {
            val resDir = selectedFile.parentFile.parentFile
            log("Determined 'res' directory: ${resDir.absolutePath}")

            val layoutName = selectedFile.nameWithoutExtension
            log("Extracted layout name: $layoutName")

            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = docBuilder.parse(selectedFile)

            val nodesWithId = doc.getElementsByTagName("*").toElementSequence()
                .filter { it.hasAttribute("android:id") }

            log("Found ${nodesWithId.toList().size} elements with 'android:id' attribute")

            for (element in nodesWithId) {
                log("Processing element with id: ${element.getAttribute("android:id")}")
                val elementId = element.getAttribute("android:id").split("/").last().toLowerCase()

                for ((attribute, translated) in attributes) {
                    val value = element.getAttribute("android:$attribute")
                    if (value.contains("dp") && !value.startsWith("@")) {
                        val baseSize = value.replace("dp", "").toInt()
                        val dimenName = "${layoutName}_${elementId}_${translated}"

                        element.setAttribute("android:$attribute", "@dimen/$dimenName")

                        val baseDimensPath = File(resDir, "values/dimens.xml")
                        log("Appending dimen to base dimens file: $dimenName = ${baseSize}dp")
                        appendDimenToFile(baseDimensPath.absolutePath, dimenName, baseSize)

                        for (sw in widthMultipliers.keys) {
                            if (isDimensionEnabled(sw)) {
                                val folderName = "values-$sw"
                                val dimensFile = File(resDir, "$folderName/dimens.xml")
                                if (dimensFile.exists()) {
                                    val factor = getWidthMultiplier(sw)
                                    val calculatedValue = calculateDimensValue(baseSize, factor)
                                    log("Appending dimen to $dimensFile: $dimenName = ${calculatedValue}dp")
                                    appendDimenToFile(dimensFile.absolutePath, dimenName, calculatedValue)
                                } else {
                                    log("Warning: $dimensFile does not exist.")
                                }
                            }
                        }
                    }
                }
            }

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "no")
            val writer = PrintWriter(FileOutputStream(selectedFile))
            val result = StreamResult(writer)
            transformer.transform(DOMSource(doc), result)

            log("Processing completed successfully.")
        } catch (e: Exception) {
            log("Error processing file: ${e.message}")
            e.printStackTrace()
        }
    }







    fun NodeList.toElementSequence(): Sequence<Element> {
        return sequence {
            for (i in 0 until length) {
                val node = item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    yield(node as Element)
                }
            }
        }
    }

    operator fun NodeList.iterator(): Iterator<Node> {
        return (0 until length).asSequence().map { item(it) }.iterator()
    }
}