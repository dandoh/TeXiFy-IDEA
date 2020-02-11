package nl.hannahsten.texifyidea.reference

import com.intellij.psi.*
import com.intellij.util.containers.toArray
import nl.hannahsten.texifyidea.index.LatexDefinitionIndex
import nl.hannahsten.texifyidea.psi.LatexCommands
import nl.hannahsten.texifyidea.util.Magic
import nl.hannahsten.texifyidea.util.childrenOfType
import nl.hannahsten.texifyidea.util.parentsOfType
import nl.hannahsten.texifyidea.util.projectSearchScope

/**
 * Command reference. When resolved, points to the command definition.
 *
 * @author Abby Berkers
 */
class CommandDefinitionReference(element: LatexCommands) : PsiReferenceBase<LatexCommands>(element), PsiPolyVariantReference {
    init {
        rangeInElement = ElementManipulators.getValueTextRange(element)
    }

    // Find all command definitions and redefinitions which define the current element
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val definitionsAndRedefinitions = Magic.Command.commandDefinitions + Magic.Command.redefinitions

        // Don't resolve to a definition when you are in a \newcommand
        if (element.parentsOfType<LatexCommands>().any { it.name in definitionsAndRedefinitions }) {
            return emptyArray()
        }
        else {
            return LatexDefinitionIndex.getCommandsByNames(definitionsAndRedefinitions, element.project, element.project.projectSearchScope)
                    .filter { it.requiredParameters.firstOrNull() == element.name }
                    .mapNotNull { newcommand ->
                        // Find the command being defined, e.g. \hi in case of \newcommand{\hi}{}
                        // We should resolve to \hi, not to \newcommand, because otherwise the find usages
                        // will try to find references to the \hi definition and won't find anything because
                        // the references point to the \newcommand.
                        val definedCommand = newcommand.childrenOfType<LatexCommands>().firstOrNull()

                        if (definedCommand == null) {
                            null
                        } else {
                            PsiElementResolveResult(definedCommand as PsiElement)
                        }
                    }
                    .toArray(emptyArray())
        }
    }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    // Check if this reference resolves to the given element
    override fun isReferenceTo(element: PsiElement): Boolean {
        return multiResolve(false).any { it.element == element }
    }
}