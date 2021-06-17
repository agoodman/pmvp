package org.pmvp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class SingularItemStorage: InMemorySingularStorage<SingularItem>()
