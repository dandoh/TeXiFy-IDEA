package nl.hannahsten.texifyidea.reference;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import nl.hannahsten.texifyidea.TexifyIcons;
import nl.hannahsten.texifyidea.completion.handlers.LatexReferenceInsertHandler;
import nl.hannahsten.texifyidea.psi.LatexCommands;
import nl.hannahsten.texifyidea.settings.LabelingCommandInformation;
import nl.hannahsten.texifyidea.settings.TexifySettings;
import nl.hannahsten.texifyidea.util.LabelsKt;
import nl.hannahsten.texifyidea.util.Magic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A reference to a label.
 * When resolved, it points to the label declaration.
 *
 * @author Hannah Schellekens, Sten Wessel
 */
public class LatexLabelReference extends PsiReferenceBase<LatexCommands> implements PsiPolyVariantReference {
    private final String key;

    public LatexLabelReference(@NotNull LatexCommands element, TextRange range) {
        super(element);
        key = range.substring(element.getText());

        // Only show Ctrl+click underline under the reference name
        setRangeInElement(range);
    }

    // Get all label definitions
    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean b) {
        Project project = myElement.getProject();
        final Collection<PsiElement> labels = LabelsKt.findLabels(project, key);
        return labels.stream().map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
    }

    // Get the label definition if there is exactly one, none otherwise
    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return Arrays.stream(multiResolve(false)).anyMatch(it -> it.getElement() == element);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        PsiFile file = myElement.getContainingFile().getOriginalFile();
        Map<String, LabelingCommandInformation> commands = TexifySettings.getInstance().getLabelCommands();

        String command = myElement.getCommandToken().getText();

        // add bibreferences to autocompletion for \cite-style commands
        if (Magic.Command.bibliographyReference.contains(command)) {
            return LabelsKt.findBibtexItems(file).stream()
                    .map(bibtexEntry -> {
                        if (bibtexEntry != null) {
                            PsiFile containing = bibtexEntry.getContainingFile();

                            if (bibtexEntry instanceof LatexCommands) {
                                LatexCommands actualCommand = (LatexCommands) bibtexEntry;
                                List<String> parameters = actualCommand.getRequiredParameters();
                                return LookupElementBuilder.create(parameters.get(0))
                                        .bold()
                                        .withInsertHandler(new LatexReferenceInsertHandler())
                                        .withTypeText(containing.getName() + ": " +
                                                        (1 + StringUtil.offsetToLineNumber(containing.getText(), bibtexEntry.getTextOffset())),
                                                true)
                                        .withIcon(TexifyIcons.DOT_BIB);
                            }
                            else {
                                return null;
                            }
                        }
                        return null;
                    }).filter(Objects::nonNull).toArray();
        }
        // add all labels to \ref-styled commands
        else if (Magic.Command.labelReference.contains(command)) {
            return LabelsKt.findLabels(file)
                    .stream()
                    .map(labelingCommand -> LookupElementBuilder
                            .create(LabelsKt.extractLabelName(labelingCommand))
                            .bold()
                            .withInsertHandler(new LatexReferenceInsertHandler())
                            .withTypeText(labelingCommand.getContainingFile().getName() + ":"
                                    + (1 + StringUtil.offsetToLineNumber(
                                    labelingCommand.getContainingFile().getText(),
                                    labelingCommand.getTextOffset())), true)
                            .withIcon(TexifyIcons.DOT_LABEL)).toArray();
        }
        // if command isn't ref or cite-styled return empty array
        return new Object[]{};
    }
}
