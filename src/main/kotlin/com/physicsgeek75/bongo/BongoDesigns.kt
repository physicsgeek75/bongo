package com.physicsgeek75.bongo

data class Design(val id: String, val name: String)

object BongoDesigns {
    fun list(clazz: Class<*>): List<Design> {
        val url = clazz.getResource("/designs/index.txt") ?: return listOf(Design("classic","Classic"))

        return try {
            url.readText().lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull {
                    val i = it.indexOf('=')
                    if (i <= 0) null else Design(it.substring(0, i), it.substring(i + 1))
                }
                .toList()
        } catch (e: Exception) {
            listOf(Design("classic","Classic"))
        }
    }
}