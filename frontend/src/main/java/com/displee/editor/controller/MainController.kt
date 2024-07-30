package com.displee.editor.controller

import com.displee.cache.CacheLibrary
import com.displee.cache.ProgressListener
import com.displee.editor.notifyChooseScriptId
import com.displee.editor.ui.alert.Notification
import com.displee.editor.ui.autocomplete.AutoCompleteItem
import com.displee.editor.ui.autocomplete.AutoCompletePopup
import com.displee.editor.ui.autocomplete.AutoCompleteUtils
import com.displee.editor.ui.autocomplete.item.AutoCompleteArgument
import com.displee.editor.ui.autocomplete.item.AutoCompleteFunction
import com.displee.editor.ui.buildStyle
import com.displee.editor.ui.window.AboutWindow
import dawn.cs2.*
import dawn.cs2.CS2Type.*
import dawn.cs2.util.FunctionDatabase
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Window
import javafx.util.Callback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.io.path.div
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class MainController : Initializable {

    @FXML
    private lateinit var openMenuItem: MenuItem

    @FXML
    private lateinit var saveScriptAs: MenuItem

    @FXML
    private lateinit var buildMenuItem: MenuItem

    @FXML
    private lateinit var exportSignatures: MenuItem

    @FXML
    private lateinit var showAssemblyMenuItem: CheckMenuItem

    @FXML
    private lateinit var newMenuItem: MenuItem

    @FXML
    private lateinit var importScriptMenuItem: MenuItem

    @FXML
    private lateinit var aboutMenuItem: MenuItem

    @FXML
    private lateinit var searchField: TextField

    @FXML
    private lateinit var rootPane: BorderPane

    @FXML
    private lateinit var statusLabel: Label

    @FXML
    private lateinit var scriptList: ListView<Int>

    @FXML
    private lateinit var mainCodePane: BorderPane

    @FXML
    private lateinit var tabPane: TabPane

    @FXML
    private lateinit var compilePane: BorderPane

    @FXML
    private lateinit var compileArea: TextArea

    @FXML
    private lateinit var assemblyCodePane: BorderPane

    private val cachedScripts = mutableMapOf<Int, String>()

    private var temporaryAssemblyPane: Node? = null

    lateinit var cacheLibrary: CacheLibrary
    private lateinit var scripts: IntArray

    private lateinit var opcodesDatabase: FunctionDatabase
    private lateinit var scriptsDatabase: FunctionDatabase

    private var currentScript: CS2? = null

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        rootPane.addEventHandler(KeyEvent.KEY_PRESSED) { e: KeyEvent ->
            when {
                e.isControlDown && e.code == KeyCode.N -> {
                    if (!this::cacheLibrary.isInitialized) {
                        return@addEventHandler
                    }
                    newScript(notifyChooseScriptId(cacheLibrary.index(SCRIPTS_INDEX).nextId()))
                }

                e.isControlDown && e.code == KeyCode.I -> {
                    if (!this::cacheLibrary.isInitialized) {
                        return@addEventHandler
                    }
                    importScript()
                }
            }
        }
        openMenuItem.setOnAction {
            openCache()
        }
        saveScriptAs.setOnAction {
            saveScriptAs()
        }
        buildMenuItem.setOnAction {
            packScript()
        }
        exportSignatures.setOnAction {
            exportSignatures()
        }
        showAssemblyMenuItem.setOnAction {
            if (showAssemblyMenuItem.isSelected) {
                rootPane.right = temporaryAssemblyPane
                temporaryAssemblyPane = null
            } else {
                temporaryAssemblyPane = rootPane.right
                rootPane.right = null
            }
        }
        newMenuItem.setOnAction {
            newScript(notifyChooseScriptId(cacheLibrary.index(SCRIPTS_INDEX).nextId()))
        }
        importScriptMenuItem.setOnAction {
            importScript()
        }
        aboutMenuItem.setOnAction {
            AboutWindow()
        }
        scriptList.cellFactory = object : Callback<ListView<Int>, ListCell<Int>> {
            override fun call(param: ListView<Int>): ListCell<Int> {
                return object : ListCell<Int>() {
                    override fun updateItem(item: Int?, empty: Boolean) {
                        super.updateItem(item, empty)
                        if (item == null) {
                            return
                        }
                        super.setText("Script $item")
                    }
                }
            }
        }
        scriptList.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            if (newValue == null) {
                return@addListener
            }
            status("decompiling script $newValue")
            currentScript = readScript(newValue)
            if (currentScript == null) {
                // TODO Popup failed to read script
                status("ready")
                return@addListener
            }
            val hash = cacheLibrary.hashCode().toString() + " - " + newValue.toString().hashCode()
            for (tab in tabPane.tabs) {
                if (tab.properties["hash"] == hash) {
                    tabPane.selectionModel.select(tab)
                    return@addListener
                }
            }
            val script = decompileScript()
            status("ready")
            val codeArea = createCodeArea(script, true)
            val tab = Tab("Script $newValue", BorderPane(VirtualizedScrollPane(codeArea)))
            tab.properties["hash"] = hash
            tab.userData = currentScript
            tabPane.tabs.add(tab)
            tabPane.selectionModel.selectLast()
            refreshAssemblyCode()
            compileScript()
        }
        searchField.textProperty().addListener { observable, oldValue, newValue ->
            if (newValue == null) {
                return@addListener
            }
            search(newValue)
        }
        tabPane.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            if (oldValue != null) {
                val codeArea =
                    ((oldValue.content as BorderPane).center as VirtualizedScrollPane<MainCodeArea>).content as MainCodeArea
                codeArea.autoCompletePopup?.hide()
            }
            if (newValue == null || newValue.userData == null) {
                if (newValue == null) {
                    replaceAssemblyCode("")
                }
                return@addListener
            }
            currentScript = newValue.userData as CS2
            refreshAssemblyCode()
        }
        assemblyCodePane.center = BorderPane(VirtualizedScrollPane(createCodeArea("", editable = false)))

        // Disable assembly pane by default
        showAssemblyMenuItem.selectedProperty().set(false)
        showAssemblyMenuItem.onAction.handle(null)

        // init singleton
        AutoCompleteUtils
    }

    private fun openCache(f: File? = null) {
        var mehFile = f
        if (mehFile == null) {
            val chooser = DirectoryChooser()
            mehFile = chooser.showDialog(mainWindow()) ?: return
        }
        scriptList.isDisable = true
        GlobalScope.launch {
            try {
                cacheLibrary = CacheLibrary(
                    mehFile.absolutePath,
                    listener = object : ProgressListener {
                        override fun notify(progress: Double, message: String?) {
                            if (message == null) {
                                return
                            }
                        }
                    }
                )
                if (!cacheLibrary.exists(SCRIPTS_INDEX)) {
                    Platform.runLater {
                        Notification.error("Can't find any scripts in the cache you trying to load.")
                        clearCache()
                    }
                    return@launch
                } else if (cacheLibrary.isRS3()) {
                    Platform.runLater {
                        Notification.error("RS3 caches are not supported.")
                        clearCache()
                    }
                }
                loadParams()
                loadScripts()
                createScriptConfigurations()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadParams() {
        status("Populating params...")
        val paramsSize = populateAttributes(cacheLibrary)
        println("Populated $paramsSize params...")
    }

    private fun loadScripts() {
        status("Populating scripts...")
        val ids = cacheLibrary.index(SCRIPTS_INDEX).archiveIds()
        scripts = ids.copyOf()
        search(searchField.text)
    }

    private fun search(text: String) {
        val list = arrayListOf<Int>()
        for (i in scripts) {
            try {
                if (text.startsWith("op_")) {
                    val data = cacheLibrary.data(SCRIPTS_INDEX, i)
                    val script = CS2Reader.readCS2ScriptNewFormat(
                        data,
                        i,
                        scriptConfiguration.unscrambled,
                        scriptConfiguration.disableSwitches,
                        scriptConfiguration.disableLongs
                    )
                    val opcode = text.replace("op_", "").toInt()
                    for (instruction in script.instructions) {
                        if (instruction.opcode == opcode) {
                            list.add(i)
                            break
                        }
                    }
                } else if (text.isNotEmpty() && !i.toString().startsWith(text)) {
                    val cached = cachedScripts[i]
                    if (cached != null && cached.contains(text)) {
                        list.add(i)
                    }
                } else {
                    list.add(i)
                }
            } catch (t: NumberFormatException) {
                list.add(i)
            }
        }
        scriptList.items.clear()
        scriptList.items.addAll(list)
    }

    private fun createCodeArea(
        initialText: String,
        showLineNumbers: Boolean = false,
        editable: Boolean = true
    ): MainCodeArea {
        val codeArea = MainCodeArea(editable)
        if (showLineNumbers) {
            codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
        }
        codeArea.buildStyle()
        val whiteSpace = Pattern.compile("^\\s+")
        codeArea.addEventHandler(KeyEvent.KEY_PRESSED) { e: KeyEvent ->
            if (e.isControlDown && e.code == KeyCode.S) {
                compileScript()
            } else if (e.isControlDown && e.code == KeyCode.D) {
                packScript()
            } else if (e.code == KeyCode.ENTER) {
                val caretPosition = codeArea.caretPosition
                val currentParagraph = codeArea.currentParagraph
                val m0 = whiteSpace.matcher(codeArea.getParagraph(currentParagraph - 1).segments[0])
                if (m0.find()) {
                    Platform.runLater { codeArea.insertText(caretPosition, m0.group()) }
                }
            } else if (e.isShiftDown && e.code == KeyCode.TAB) {
                if (codeArea.text.substring(codeArea.caretPosition - 1, codeArea.caretPosition) == "\t") {
                    codeArea.deletePreviousChar()
                }
            }
        }
        codeArea.isEditable = editable
        codeArea.replaceText(0, 0, initialText)
        return codeArea
    }

    private fun createScriptConfigurations() {
        status("guessing script configuration...")
        opcodesDatabase = FunctionDatabase.createOpcodeDatabase()
        status("generating script signatures...")
        scriptsDatabase = scriptConfiguration.generateScriptsDatabase(cacheLibrary)
        status("generating auto complete items...")
        generateAutoCompleteItems()
        status("caching all scripts...")
        cacheAllScripts()
        status("ready")
        Platform.runLater {
            scriptList.isDisable = false
            newMenuItem.isDisable = false
            importScriptMenuItem.isDisable = false
            saveScriptAs.isDisable = false
            buildMenuItem.isDisable = false
            exportSignatures.isDisable = false
        }
    }

    private fun generateAutoCompleteItems() {
        AutoCompleteUtils.dynamicItems.clear()
        AutoCompleteUtils.clearDynamicChildren()
        val text = javaClass.getResource(scriptConfiguration.opcodeDatabase).readText()
        for (line in text.lines()) {
            if (line.isEmpty() || line.startsWith(" ") || line.startsWith("//") || line.startsWith("#")) {
                continue
            }
            val split = line.split(" ")
            val opcode = split[0].toInt()
            if (!scriptConfiguration.scrambled.containsKey(opcode)) {
                continue
            }
            var list: MutableList<AutoCompleteItem>? = AutoCompleteUtils.dynamicItems
            if (FlowBlocksGenerator.isObjectOpcode(opcode) || FlowBlocksGenerator.isObjectWidgetOpcode(opcode)) {
                list = AutoCompleteUtils.getObject(WIDGET_PTR, true)?.dynamicChildren
            }
            val name = split[1]
            val returnTypes = if (split[2].contains("|")) {
                val multiReturn = split[2].split("\\|".toRegex())
                Array(multiReturn.size) {
                    forDesc(multiReturn[it])
                }
            } else {
                arrayOf(forDesc(split[2]))
            }
            val argSize = (split.size - 2) / 2
            val argTypes = Array(argSize) {
                val index = 3 + (it * 2)
                forDesc(split[index])
            }
            val argNames = Array(argSize) {
                val index = 3 + (it * 2)
                split[index + 1]
            }
            val function = AutoCompleteFunction(
                name,
                returnTypes[0],
                Array(argSize) {
                    AutoCompleteArgument(argNames[it], argTypes[it])
                }
            )
            if (list != null && list.firstOrNull { it.name == name } == null) {
                list.add(function)
            }
        }
    }

    private fun readScript(id: Int): CS2? {
        val data = cacheLibrary.data(SCRIPTS_INDEX, id)
        return CS2Reader.readCS2ScriptNewFormat(
            data,
            id,
            scriptConfiguration.unscrambled,
            scriptConfiguration.disableSwitches,
            scriptConfiguration.disableLongs
        )
    }

    private fun cacheAllScripts() {
        cachedScripts.clear()
        for (i in scripts) {
            try {
                val decompiled = decompileScript(i)
                cachedScripts[i] = decompiled
            } catch (e: java.lang.Exception) {
            }
        }
    }

    private fun decompileScript(): String {
        val script = currentScript ?: return ""
        val decompiler = CS2Decompiler(script, opcodesDatabase, scriptsDatabase)
        try {
            decompiler.decompile()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        decompiler.optimize()
        val printer = CodePrinter()
        decompiler.function.print(printer)
        return printer.toString()
    }

    private fun decompileScript(id: Int): String {
        val data = cacheLibrary.data(SCRIPTS_INDEX, id)
        val script = CS2Reader.readCS2ScriptNewFormat(
            data,
            id,
            scriptConfiguration.unscrambled,
            scriptConfiguration.disableSwitches,
            scriptConfiguration.disableLongs
        )
        val decompiler = CS2Decompiler(script, opcodesDatabase, scriptsDatabase)
        try {
            decompiler.decompile()
        } catch (t: Throwable) {
            // t.printStackTrace()
        }
        decompiler.optimize()
        val printer = CodePrinter()
        decompiler.function.print(printer)
        return printer.toString()
    }

    private fun saveScript() {
        val script = currentScript ?: return
        val activeCodeArea = activeCodeArea()

        val outputDir = DirectoryChooser().showDialog(mainWindow()) ?: return
        val outputFile = outputDir.toPath() / "${script.scriptID}.cs2"
        outputFile.writeText(activeCodeArea.text)

        println("Saved script ${script.scriptID}.cs2")
    }

    private fun saveScriptAs() {
        val script = currentScript ?: return
        val activeCodeArea = activeCodeArea()

        val fileChooser = FileChooser().apply {
            title = "Save"
            initialFileName = "${script.scriptID}"

            extensionFilters.add(FileChooser.ExtensionFilter("Compiled (.dat)", "*.dat"))
            extensionFilters.add(FileChooser.ExtensionFilter("Source (.cs2)", "*.cs2"))
        }

        val outputFile = fileChooser.showSaveDialog(mainWindow()) ?: return

        when (outputFile.extension) {
            "dat" -> {
                val function =
                    CS2ScriptParser.parse(activeCodeArea.text.removeCommentedLines(), opcodesDatabase, scriptsDatabase)
                val compiler = CS2Compiler(function)
                val compiled = compiler.compile(null) ?: throw Error("Failed to compile.")
                outputFile.writeBytes(compiled)
            }

            "cs2" -> outputFile.writeText(activeCodeArea.text)
            else -> throw IllegalStateException("Invalid ")
        }
        println("Saved script ${outputFile.name}")
    }

    private fun compileScript() {
        val script = currentScript ?: return
        val activeCodeArea = activeCodeArea()
        try {
            val parser =
                CS2ScriptParser.parse(activeCodeArea.text.removeCommentedLines(), opcodesDatabase, scriptsDatabase)
            activeCodeArea.autoCompletePopup?.init(parser)
            refreshAssemblyCode()
            printConsoleMessage("Compiled script ${script.scriptID}.")
        } catch (t: Throwable) {
            t.printStackTrace()
            printConsoleMessage(t.message)
        }
    }

    private fun String.removeCommentedLines(): String {
        return this.lines().filter { !it.trimIndent().startsWith("//") }.joinToString("\n")
    }

    private fun newScript(newId: Int?) {
        if (newId == null) {
            return
        }
        val function = CS2ScriptParser.parse("void script_$newId() {\n\treturn;\n}", opcodesDatabase, scriptsDatabase)
        val compiler = CS2Compiler(
            function,
            scriptConfiguration.scrambled,
            scriptConfiguration.disableSwitches,
            scriptConfiguration.disableLongs
        )
        val compiled = compiler.compile(null) ?: throw Error("Failed to compile.")
        cacheLibrary.put(SCRIPTS_INDEX, newId, compiled)

        if (!cacheLibrary.index(SCRIPTS_INDEX).update()) {
            Notification.error("Failed to create new script with id $newId.")
        } else {
            Notification.info("A new script has been created with id $newId.")
            loadScripts()
        }
    }

    private fun importScript() {
        val fileChooser = FileChooser().apply {
            title = "Import Script"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("Script Files", "*.cs2"),
                FileChooser.ExtensionFilter("Compiled Script Files", "*.dat")
            )
        }
        val file = fileChooser.showOpenDialog(mainWindow()) ?: return
        try {
            val fileName = file.name
            val scriptId: Int = extractScriptIdFromFileName(fileName)
            val isUpdateSuccessful: Boolean
            when {
                fileName.endsWith(".cs2") -> {
                    val scriptContent = file.readText()
                    val function = CS2ScriptParser.parse(scriptContent, opcodesDatabase, scriptsDatabase)
                    val compiler = CS2Compiler(function)
                    val compiled = compiler.compile(null) ?: throw IllegalStateException("Failed to compile.")
                    cacheLibrary.put(SCRIPTS_INDEX, scriptId, compiled)
                    isUpdateSuccessful = cacheLibrary.index(SCRIPTS_INDEX).update()
                    Notification.info("Successfully imported script with ID $scriptId.")
                }

                fileName.endsWith(".dat") -> {
                    val compiledData = file.readBytes()
                    cacheLibrary.put(SCRIPTS_INDEX, scriptId, compiledData)
                    isUpdateSuccessful = cacheLibrary.index(SCRIPTS_INDEX).update()
                    Notification.info("Successfully imported compiled script with ID $scriptId.")
                }

                else -> {
                    Notification.error("Unsupported file type: $fileName")
                    return
                }
            }
            if (isUpdateSuccessful) {
                loadScripts()
            } else {
                Notification.error("Failed to import script.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Notification.error("Failed to import script: ${e.message}")
        }
    }

    private fun extractScriptIdFromFileName(fileName: String): Int {
        val baseName = fileName.substringBeforeLast(".")
        return baseName.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid file name format: $fileName. Expected format: <id>.cs2 or <id>.dat")
    }

    private fun packScript() {
        val script = currentScript ?: return
        val activeCodeArea = activeCodeArea()
        try {
            val function =
                CS2ScriptParser.parse(activeCodeArea.text.removeCommentedLines(), opcodesDatabase, scriptsDatabase)
            val compiler = CS2Compiler(
                function,
                scriptConfiguration.scrambled,
                scriptConfiguration.disableSwitches,
                scriptConfiguration.disableLongs
            )
            val compiled = compiler.compile(null) ?: throw Error("Failed to compile.")
            cacheLibrary.put(SCRIPTS_INDEX, script.scriptID, compiled)
            activeCodeArea.autoCompletePopup?.init(function)
            if (cacheLibrary.index(SCRIPTS_INDEX).update()) {
                printConsoleMessage("Packed script ${script.scriptID} successfully.")
            } else {
                printConsoleMessage("Failed to pack script ${script.scriptID}.")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            printConsoleMessage(t.message)
        }
    }

    private fun exportSignatures() {
        val outputDir = DirectoryChooser().showDialog(mainWindow()) ?: return
        val outputFile = outputDir.toPath() / "script-signatures.txt"

        printConsoleMessage("Generating signatures")
        val database = scriptConfiguration.generateScriptsDatabase(cacheLibrary, loop = 10)

        outputFile.writeText(database.source)
        Notification.info("Successfully exported signatures to $outputFile.")
    }

    private fun printConsoleMessage(line: String?) {
        compileArea.text =
            timeFormat.format(Date.from(Instant.now())) + " -> " + line + System.lineSeparator() + compileArea.text
    }

    private fun refreshAssemblyCode() {
        try {
            val parser = CS2ScriptParser.parse(activeCodeArea().text, opcodesDatabase, scriptsDatabase)
            val compiler = CS2Compiler(
                parser,
                scriptConfiguration.scrambled,
                scriptConfiguration.disableSwitches,
                scriptConfiguration.disableLongs
            )
            val stringWriter = StringWriter()
            val writer = PrintWriter(stringWriter)
            compiler.compile(writer)
            replaceAssemblyCode(stringWriter.toString())
        } catch (e: Exception) {
            // do nothing
            // replaceAssemblyCode("Failed to generate assembly code.")
        }
    }

    private fun replaceAssemblyCode(code: String) {
        val assemblyCodeArea =
            ((assemblyCodePane.center as BorderPane).center as VirtualizedScrollPane<CodeArea>).content as CodeArea
        assemblyCodeArea.replaceText(0, assemblyCodeArea.length, code)
    }

    private fun clearCache() {
        tabPane.tabs.clear()
        scriptList.items.clear()
        scriptList.isDisable = true
        newMenuItem.isDisable = true
        saveScriptAs.isDisable = true
        buildMenuItem.isDisable = true
        exportSignatures.isDisable = true
    }

    private fun status(status: String) {
        Platform.runLater {
            statusLabel.text = "Status: $status"
        }
    }

    private fun activeCodeArea(): MainCodeArea {
        return ((tabPane.selectionModel.selectedItem.content as BorderPane).center as VirtualizedScrollPane<CodeArea>).content as MainCodeArea
    }

    fun mainWindow(): Window {
        return mainCodePane.scene.window
    }

    companion object {

        var timeFormat = SimpleDateFormat("HH:mm:ss")
        val VAR_LIST = mutableListOf<String>()
        val KEYWORDS = arrayOf(
            "string",
            "string[]",
            "boolean",
            "break",
            "case",
            "char",
            "continue",
            "default",
            "do",
            "else",
            "for",
            "goto",
            "if",
            "int",
            "int[]",
            "long",
            "long[]",
            "return",
            "switch",
            "this",
            "void",
            "while",
            "true",
            "false",
            "null"
        )

        private val BI_CLASSES = arrayOf(
            FONTMETRICS,
            SPRITE,
            MODEL,
            ENUM,
            STRUCT,
            CONTAINER,
            WIDGET_PTR,
            LOCATION,
            ITEM_ID,
            ITEM,
            NAMED_ITEM,
            COLOR,
            ANIM,
            MAPID,
            GRAPHIC,
            SKILL,
            NPCDEF,
            TEXTURE,
            CATEGORY,
            SOUNDEFFECT,
            CALLBACK,
            DB_ROW,
            DB_COLUMN,
            DB_FIELD,
            DB_TABLE,
            OBJECT,
            MAP_ELEMENT,
            AREA,
            LOCSHAPE,
            NPCUID,
            OVERLAY_INTERFACE,
            TOPLEVEL_INTERFACE
        )

        private val KEYWORD_PATTERN = "\\b(" + java.lang.String.join(
            "|",
            *KEYWORDS.map { it.replace("[", "\\[").replace("]", "\\]") }.toTypedArray()
        ) + ")\\b"
        private var VAR_PATTERN = "\\b(" + java.lang.String.join("|", *VAR_LIST.toTypedArray()) + ")\\b"
        private val BICLASS_PATTERN = "\\b(" + java.lang.String.join(
            "|",
            *BI_CLASSES.map { it.name.replace("[", "\\[").replace("]", "\\]") }.toTypedArray()
        ) + ")\\b"

        private const val PAREN_PATTERN = "\\(|\\)"
        private const val BRACE_PATTERN = "\\{|\\}"
        private const val BRACKET_PATTERN = "\\[|\\]"
        private const val SEMICOLON_PATTERN = "\\;"
        private const val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
        private const val NUMBER_PATTERN = "\\b\\d+\\b"
        private const val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
        private const val COLOR_PATTERN = "0x.{3,6}\\b"

        // TODO Fix these highlightings
        private val CS2_CALL_PATTERN = "\\bscript_\\d+(.*)\\b"
        private val CS2_HOOK_PATTERN = "\\b&script_\\d+(.*)"

        fun computeHighlighting(text: String): StyleSpans<Collection<String>>? {
            VAR_PATTERN = "\\b(" + java.lang.String.join("|", *VAR_LIST.toTypedArray()) + ")\\b"
            val pattern =
                Pattern.compile("(?<KEYWORD>$KEYWORD_PATTERN)|(?<BICLASS>$BICLASS_PATTERN)|(?<NUMBER>$NUMBER_PATTERN)|(?<PAREN>$PAREN_PATTERN)|(?<BRACE>$BRACE_PATTERN)|(?<BRACKET>$BRACKET_PATTERN)|(?<SEMICOLON>$SEMICOLON_PATTERN)|(?<STRING>$STRING_PATTERN)|(?<COMMENT>$COMMENT_PATTERN)|(?<COLOR>$COLOR_PATTERN)|(?<VAR>$VAR_PATTERN)"/*|(?<CS2CALL>$CS2_CALL_PATTERN)|(?<CS2HOOK>$CS2_HOOK_PATTERN)"*/)
            val matcher: Matcher = pattern.matcher(text)
            var lastKwEnd = 0
            val spansBuilder = StyleSpansBuilder<Collection<String>>()
            while (matcher.find()) {
                val styleClass = when {
                    matcher.group("KEYWORD") != null -> "keyword"
                    matcher.group("PAREN") != null -> "paren"
                    matcher.group("BRACE") != null -> "brace"
                    matcher.group("BRACKET") != null -> "bracket"
                    matcher.group("SEMICOLON") != null -> "semicolon"
                    matcher.group("STRING") != null -> "string"
                    matcher.group("NUMBER") != null -> "number"
                    matcher.group("COMMENT") != null -> "comment"
                    matcher.group("BICLASS") != null -> "biclass"
                    /*matcher.group("CS2CALL") != null -> "cs2-call"
                    matcher.group("CS2HOOK") != null -> "cs2-hook"*/
                    matcher.group("COLOR") != null -> "color"
                    matcher.group("VAR") != null -> "var"
                    else -> null
                }
                if (styleClass == null) {
                    continue
                }
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd)
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start())
                lastKwEnd = matcher.end()
            }
            spansBuilder.add(Collections.emptyList(), text.length - lastKwEnd)
            return spansBuilder.create()
        }
    }

    private class MainCodeArea(autoComplete: Boolean = true) : CodeArea() {
        val autoCompletePopup: AutoCompletePopup? = if (autoComplete) AutoCompletePopup(this) else null
    }
}