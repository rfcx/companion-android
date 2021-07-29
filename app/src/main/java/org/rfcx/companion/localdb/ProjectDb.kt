package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Project.Companion.PROJECT_DELETED_AT
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.entity.response.permissionsLabel
import org.rfcx.companion.entity.response.toLocationGroups
import java.util.*

class ProjectDb(private val realm: Realm) {
    fun insertOrUpdateProject(group: Project) {
        realm.executeTransaction {
            if (group.id == 0) {
                val id =
                    (realm.where(Project::class.java).max(Project.PROJECT_ID)
                        ?.toInt() ?: 0) + 1
                group.id = id
            }
            it.insertOrUpdate(group)
        }
    }

    fun unsentCount(): Long {
        return realm.where(Project::class.java)
            .notEqualTo(Project.PROJECT_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun unlockSent(): List<Project> {
        var unsentCopied: List<Project> = listOf()
        realm.executeTransaction {
            val unsent = it.where(Project::class.java)
                .equalTo(Project.PROJECT_SYNC_STATE, SyncState.Unsent.key).findAll()
                .createSnapshot()
            unsentCopied = unsent.toList()
            unsent.forEach { d ->
                d.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    fun unlockSending() {
        realm.executeTransaction {
            val snapshot = it.where(Project::class.java)
                .equalTo(Project.PROJECT_SYNC_STATE, SyncState.Sending.key).findAll()
                .createSnapshot()
            snapshot.forEach {
                it.syncState = SyncState.Unsent.key
            }
        }
    }

    fun markUnsent(id: Int) {
        mark(id = id, syncState = SyncState.Unsent.key)
    }

    fun markSent(serverId: String, id: Int) {
        mark(id, serverId, SyncState.Sent.key)
    }

    fun getProjects(): List<Project> {
        return realm.where(Project::class.java).isNull(PROJECT_DELETED_AT)
            .sort(Project.PROJECT_NAME, Sort.ASCENDING).findAll()
            ?: arrayListOf()
    }

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Project> {
        return realm.where(Project::class.java)
            .sort(Project.PROJECT_ID, sort)
            .findAllAsync()
    }

    fun getProjectByName(name: String): Project? {
        return realm.where(Project::class.java)
            .equalTo(Project.PROJECT_NAME, name).findFirst()
    }

    fun getProjectById(id: Int): Project? {
        return realm.where(Project::class.java)
            .equalTo(Project.PROJECT_ID, id).findFirst()
    }

    fun insertOrUpdate(groupsResponse: ProjectResponse) {
        realm.executeTransaction {
            val project =
                it.where(Project::class.java)
                    .equalTo(Project.PROJECT_SERVER_ID, groupsResponse.id)
                    .findFirst()

            if (project == null) {
                val projectObject = groupsResponse.toLocationGroups()
                val id =
                    (it.where(Project::class.java).max(Project.PROJECT_ID)
                        ?.toInt() ?: 0) + 1
                projectObject.id = id
                it.insert(projectObject)
            } else if (project.syncState == SyncState.Sent.key) {
                project.serverId = groupsResponse.id
                project.name = groupsResponse.name
                project.color = groupsResponse.color
                project.permissions = groupsResponse.permissionsLabel()
            }
        }
    }

    fun deleteProjectsByCoreId(coreIds: List<String>) {
        realm.executeTransaction {
            coreIds.forEach { id ->
                val project =
                    it.where(Project::class.java)
                        .equalTo(Project.PROJECT_SERVER_ID, id)
                        .findFirst()

                project?.deleteFromRealm()
            }
        }
    }

    fun deleteProjectInLocal(id: Int) {
        realm.executeTransaction {
            val project =
                it.where(Project::class.java).equalTo(Project.PROJECT_ID, id)
                    .findFirst()
            project?.deleteFromRealm()
        }
    }

    fun isExisted(name: String?): Boolean {
        return if (name != null) {
            val project = realm.where(Project::class.java)
                .equalTo(Project.PROJECT_NAME, name).findFirst()
            project != null
        } else {
            false
        }
    }

    private fun mark(id: Int, serverId: String? = null, syncState: Int) {
        realm.executeTransaction {
            val project =
                it.where(Project::class.java).equalTo(Project.PROJECT_ID, id)
                    .findFirst()
            if (project != null) {
                project.serverId = serverId
                project.syncState = syncState
            }
        }
    }
}
