package com.github.abdulaziz.hotfolder

import com.github.abdulaziz.hotfolder.services.HotFolderService
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HotFolderServiceTest : BasePlatformTestCase() {

    fun testAddHotFolder() {
        val service = HotFolderService.getInstance(project)
        val path = "/tmp/test-folder"
        service.addHotFolder(path)
        
        assertTrue(service.getHotFolders().contains(path))
    }

    fun testRemoveHotFolder() {
        val service = HotFolderService.getInstance(project)
        val path = "/tmp/test-folder"
        service.addHotFolder(path)
        service.removeHotFolder(path)
        
        assertFalse(service.getHotFolders().contains(path))
    }
}
