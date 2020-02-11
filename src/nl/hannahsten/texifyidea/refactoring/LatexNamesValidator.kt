package nl.hannahsten.texifyidea.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project

class LatexNamesValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project?): Boolean {
        return false
    }

    override fun isIdentifier(name: String, project: Project?): Boolean {
        return true
    }
}