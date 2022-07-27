package org.rfcx.companion.localdb

import io.realm.Realm
import org.rfcx.companion.entity.guardian.Classifier
import org.rfcx.companion.util.RealmLiveData
import org.rfcx.companion.util.asLiveData

class ClassifierDb(private val realm: Realm) {

    fun mock() {
        realm.executeTransaction {
            it.insertOrUpdate(
                Classifier(
                    id = "1637901623152",
                    name = "elephant",
                    version = "2",
                    path = "/data/user/0/org.rfcx.companion/files/classifiers/1637901623151.tflite",
                    sha1 = "69482d8b65083e2fabcf1096033c863409cc50f7",
                    sampleRate = "8000",
                    inputGain = "1.0",
                    windowSize = "2.5000",
                    stepSize = "2.0000",
                    classifications = "elephant,environment",
                    classificationsFilterThreshold = "0.98,1.00"
                )
            )
        }
    }

    fun get(id: String): Classifier? {
        val classifier = realm.where(Classifier::class.java)
            .equalTo(Classifier.FIELD_ID, id)
            .findFirst() ?: return null
        return realm.copyFromRealm(classifier)
    }

    fun getAll(): List<Classifier>? {
        val classifiers = realm.where(Classifier::class.java)
            .findAll()
        return realm.copyFromRealm(classifiers)
    }

    fun getAllAsLiveData(): RealmLiveData<Classifier> {
        val classifiers = realm.where(Classifier::class.java)
            .findAll()
        return classifiers.asLiveData()
    }

    fun insert(classifier: Classifier) {
        realm.executeTransaction {
            it.insertOrUpdate(classifier)
        }
    }

    fun delete(id: String) {
        realm.executeTransaction {
            it.where(Classifier::class.java)
                .equalTo(Classifier.FIELD_ID, id)
                .findAll()
                .deleteAllFromRealm()
        }
    }
}
