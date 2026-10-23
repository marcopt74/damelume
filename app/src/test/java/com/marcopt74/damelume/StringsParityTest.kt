package com.marcopt74.damelume

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringsParityTest {
    @Test
    fun valuesAndValuesPtHaveTheSameKeys() {
        val english = keys(resourceFile("values/strings.xml"))
        val portuguese = keys(resourceFile("values-pt/strings.xml"))
        assertThat(english).isNotEmpty()
        assertThat(portuguese).isEqualTo(english)
    }

    private fun keys(file: File): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val names = mutableSetOf<String>()
        val nodes = document.getElementsByTagName("*")
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node !is Element) continue
            if (node.tagName == "string" || node.tagName == "plurals") {
                names += node.getAttribute("name")
            }
        }
        return names
    }

    private fun resourceFile(relative: String): File {
        val candidates =
            listOf(
                File("src/main/res/$relative"),
                File("app/src/main/res/$relative"),
            )
        return candidates.firstOrNull { it.exists() }
            ?: error("Missing $relative. Tried ${candidates.joinToString { it.path }}")
    }
}
