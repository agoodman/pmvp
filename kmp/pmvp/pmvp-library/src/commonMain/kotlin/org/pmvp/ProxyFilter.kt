package org.pmvp

interface ProxyFilter<K, T : Proxy<K>> {
    fun filter(proxy: T): T
}
