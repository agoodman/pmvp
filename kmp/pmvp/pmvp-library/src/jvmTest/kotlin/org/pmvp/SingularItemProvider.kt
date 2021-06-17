package org.pmvp

data class SingularItem(val value: String)

class SingularItemProvider(
    storage: SingularStorage<SingularItem>
) : SingularProvider<SingularItem>(storage = storage)
