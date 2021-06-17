package org.pmvp

open class EmptyProxyFilter<K, T : Proxy<K>>() :
    ProxyFilter<K, T> {
    override fun filter(proxy: T): T {
        return proxy
    }
}
