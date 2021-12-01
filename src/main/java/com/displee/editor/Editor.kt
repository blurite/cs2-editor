package com.displee.editor

import com.displee.editor.controller.MainController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import java.awt.AWTException
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.io.IOException
import javax.imageio.ImageIO

lateinit var mainController: MainController

fun main(vararg args: String) {
	Application.launch(Editor::class.java, *args)
}

fun loadFXML(resource: String, controllerClass: Class<out Initializable>): Parent {
	val loader = FXMLLoader(Editor::class.java.getResource(resource))
	loader.setController(controllerClass.getDeclaredConstructor().newInstance())
	return loader.load()
}


fun setIcons(stage: Stage) {
	//checking for support
	if (!SystemTray.isSupported()) {
		println("System tray is not supported")
		return
	}

	//get the systemTray of the system
	val systemTray = SystemTray.getSystemTray()

	//get default toolkit
	//Toolkit toolkit = Toolkit.getDefaultToolkit();
	//get image
	//Toolkit.getDefaultToolkit().getImage("src/resources/busylogo.jpg");
	var image: Image? = null
	try {
		image = ImageIO.read(DummyMain::class.java.getResourceAsStream("/images/icon.png"))
		stage.icons.add(javafx.scene.image.Image(DummyMain::class.java.getResourceAsStream("/images/icon.png")))
	} catch (e: IOException) {
		e.printStackTrace()
	}

	//setting tray icon
	val trayIcon = TrayIcon(image, "Cs2 Editor")

	//adjust to default size as per system recommendation
	trayIcon.isImageAutoSize = true
	try {
		systemTray.add(trayIcon)
	} catch (awtException: AWTException) {
		awtException.printStackTrace()
	}
}

class Editor : Application() {

	override fun start(stage: Stage) {
		mainController = MainController()
		val loader = FXMLLoader(Editor::class.java.getResource("/fxml/main.fxml"))
		loader.setController(mainController)
		val root = loader.load<Parent>()

		stage.scene = Scene(root, 1200.0, 800.0).also {
			it.stylesheets.add("/css/theme.css")
			it.stylesheets.add("/css/custom.css")
			it.stylesheets.add("/css/highlight.css")
			it.stylesheets.add("/css/code-area-ui.css")
		}
		stage.title = "Displee's CS2 editor"
		setIcons(stage)
		stage.show()
	}

}