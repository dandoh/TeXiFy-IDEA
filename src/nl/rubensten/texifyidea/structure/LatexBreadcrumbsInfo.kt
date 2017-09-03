package nl.rubensten.texifyidea.structure

import com.intellij.psi.PsiElement
import com.intellij.xml.breadcrumbs.BreadcrumbsInfoProvider
import nl.rubensten.texifyidea.LatexLanguage
import nl.rubensten.texifyidea.psi.LatexCommands
import nl.rubensten.texifyidea.psi.LatexEnvironment
import nl.rubensten.texifyidea.util.name

/**
 * @author Ruben Schellekens
 */
open class LatexBreadcrumbsInfo : BreadcrumbsInfoProvider() {

    override fun getLanguages() = arrayOf(LatexLanguage.INSTANCE)

    override fun getElementInfo(element: PsiElement) = when (element) {
        is LatexEnvironment -> element.name()?.text
        is LatexCommands -> element.commandToken.text
        else -> ""
    } ?: ""

    override fun acceptElement(element: PsiElement) = when (element) {
        is LatexEnvironment -> true
        is LatexCommands -> true
        else -> false
    }
}