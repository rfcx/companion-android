package org.rfcx.companion.entity

import org.rfcx.companion.adapter.UnsyncedWorksViewItem
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.guardian.GuardianRegistration

data class UnsyncedWork(
    val deployments: List<Deployment>? = null,
    val registrations: List<GuardianRegistration>? = null
) {
    fun toAdapterItem(dpErrors: List<UnsyncedDeployment>, rgErrors: List<RegisterGuardian>): List<UnsyncedWorksViewItem> {
        val list = mutableListOf<UnsyncedWorksViewItem>()
        if (!deployments.isNullOrEmpty()) {
            list.add(UnsyncedWorksViewItem.Header("Deployment"))
            deployments.forEach {
                val dp = dpErrors.find { error -> error.id == it.id }
                list.add(UnsyncedWorksViewItem.Deployment(it.id, it.stream?.name ?: "", it.deployedAt, dp?.error))
            }
        }
        if (!registrations.isNullOrEmpty()) {
            list.add(UnsyncedWorksViewItem.Header("Registration"))
            registrations.forEach {
                val rg = rgErrors.find { error -> error.guid == it.guid }
                list.add(UnsyncedWorksViewItem.Registration(it.guid, rg?.error))
            }
        }
        return list
    }
}
