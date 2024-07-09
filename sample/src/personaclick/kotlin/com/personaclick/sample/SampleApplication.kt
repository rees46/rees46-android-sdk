package com.personaclick.sample

import com.personaclick.sdk.Personaclick
import com.personalizatio.sample.AbstractSampleApplication

class SampleApplication : AbstractSampleApplication<Personaclick>(Personaclick.getInstance()) {
    override val shopId: String
        get() = "cb0516af5da25b1b41490072e679bc"
    override val shopSecretKey: String
        get() = "secret_key"

    override fun initialize() {
        Personaclick.initialize(applicationContext, shopId, shopSecretKey)
    }
}
