package org.rfcx.audiomoth.util

import androidx.lifecycle.LiveData
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.RealmResults

fun <T : RealmModel> RealmResults<T>.asLiveData() = RealmLiveData(this)
fun <T : RealmObject> T.asLiveData() = LiveDataRealmObject(this)

class RealmLiveData<T : RealmModel>(private val realmResults: RealmResults<T>) : LiveData<RealmResults<T>>() {

    private val listener = RealmChangeListener<RealmResults<T>> { results -> value = results }

    override fun onActive() {
        realmResults.addChangeListener(listener)
    }

    override fun onInactive() {
        realmResults.removeChangeListener(listener)
    }
}

class LiveDataRealmObject<T : RealmObject>(private val result: T) : LiveData<T>() {

    private val listener = RealmChangeListener<T> { t ->
        if (t.isValid)
            value = t

    }

    override fun onActive() {
        result.addChangeListener(listener)
    }

    override fun onInactive() {
        result.removeChangeListener(listener)
    }
}