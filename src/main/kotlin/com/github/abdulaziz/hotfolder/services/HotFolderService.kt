package com.github.aamurad.hotfolder.services

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ide.projectView.ProjectView

@Service(Service.Level.PROJECT)
@State(name = "HotFolderState", storages = [Storage("hotfolder.xml")])
class HotFolderService(private val project: Project) : PersistentStateComponent<HotFolderService.State> {

    class State {
        var hotFolders: MutableList<String> = mutableListOf()
    }

    private var myState = State()
    private val watchRequests = mutableMapOf<String, LocalFileSystem.WatchRequest>()

    init {
        // Listen to VFS changes and refresh project view when hot folder contents change
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val hotFolderPaths = myState.hotFolders
                val shouldRefresh = events.any { event ->
                    val eventPath = event.path
                    hotFolderPaths.any { hotFolderPath ->
                        eventPath.startsWith(hotFolderPath)
                    }
                }
                if (shouldRefresh) {
                    ProjectView.getInstance(project).refresh()
                }
            }
        })
    }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
        // Request VFS to watch hot folder paths
        for (path in myState.hotFolders) {
            val watchRequest = LocalFileSystem.getInstance().addRootToWatch(path, true)
            if (watchRequest != null) {
                watchRequests[path] = watchRequest
            }
        }
    }

    fun addHotFolder(path: String) {
        if (!myState.hotFolders.contains(path)) {
            myState.hotFolders.add(path)
            // Add to VFS watch list
            val watchRequest = LocalFileSystem.getInstance().addRootToWatch(path, true)
            if (watchRequest != null) {
                watchRequests[path] = watchRequest
            }
        }
    }

    fun removeHotFolder(path: String) {
        myState.hotFolders.remove(path)
        // Remove from VFS watch list
        watchRequests[path]?.let { request ->
            LocalFileSystem.getInstance().removeWatchedRoot(request)
            watchRequests.remove(path)
        }
    }

    fun getHotFolders(): List<String> = myState.hotFolders

    companion object {
        fun getInstance(project: Project): HotFolderService = project.service()
    }
}
