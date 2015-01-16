package pl.edu.agh.mindmap.merge;

import org.xmind.core.CoreException;
import org.xmind.core.ISheet;
import org.xmind.core.IWorkbook;
import org.xmind.core.style.IStyle;
import org.xmind.core.style.IStyleSheet;
import pl.edu.agh.mindmap.Utils;

import java.io.IOException;
import java.util.*;

/**
 * Helper class for merging of two workbooks into one.
 * Source and target workbooks remain unchanged and should not be modified during merge!
 * The result of the merge is initially a clone of the target workbook.
 * In process of merging, elements which exist in source workbook but not in target workbook are automatically copied to result workbook.
 * Elements which exist in both source and target workbooks are returned as conflicts.
 * Conflict is only an indication that two elements differ in properties, but conflict resolution has to be done by manually
 * by modifying properties of result element. Conflicts cannot be resolved automatically because
 * they have to be decided by the user.
 */
public class WorkbookMerger extends Conflict {
    private static final String CLONING_FAILED = "Cloning target workbook failed";

    private IWorkbook source, target, result;
    private List<SheetMerger> sheetMergers;
    /**
     * @param source workbook from which changes will be added to result workbook.
     * @param target workbook which will be cloned and changes will be added from source workbook to result workbook (clone of target workbook).
     * @throws WorkbookCloningException thrown if target workbook could not be cloned.
     */
    public WorkbookMerger(IWorkbook source, IWorkbook target) throws WorkbookCloningException {
        if (source == null || target == null)
            throw new IllegalArgumentException("Null sheet passed");
        this.source = source;
        this.target = target;
        try {
            result = Utils.cloneWorkbook(target);
        } catch (IOException e) {
            throw new WorkbookCloningException(CLONING_FAILED, e);
        } catch (CoreException e) {
            throw new WorkbookCloningException(CLONING_FAILED, e);
        }
    }

    /**
     * Copies all the sheets which exist in source workbook to result workbook.
     * Returns a list of sheet mergers. One sheet merger for each sheet which exists in both source and target workbook.
     */
    public List<SheetMerger> mergeSheets() {
        if(sheetMergers !=null)
            return sheetMergers;

        List<Conflict<ISheet>> sheetConflicts = MergeUtils.findCorrespondingElements(source.getSheets(), target.getSheets(), result.getSheets());
        List<SheetMerger> sheetMergers = new LinkedList<SheetMerger>();

        for (Conflict<ISheet> conflict : sheetConflicts) {
            if (conflict.getSource() != null
                    && conflict.getTarget() != null) {
                if (conflict.getSource().getModifiedTime() != conflict.getTarget().getModifiedTime()) {
                    sheetMergers.add(new SheetMerger(conflict));
                }
            } else if (conflict.getSource() != null) {
                result.addSheet((ISheet) result.importElement(conflict.getSource()));
            }
        }
        this.sheetMergers = sheetMergers;
        return sheetMergers;
    }

    /**
     * Returns the result of merge.
     */
    public IWorkbook getResult() {
        return result;
    }

    /**
     * Copies all styles, which exist only in source workbook, to result workbook.
     * Returns a map containing 3 lists of style conflicts. Each list contains conflicts of styles of certain type.
     * Styles are in conflict if they have the same ID but differ in properties.
     * There are 3 types of styles represented by string constants: IStyleSheet.NORMAL_STYLES,
     * IStyleSheet.AUTOMATIC_STYLES and IStyleSheet.MASTER_STYLES. Lists are stored in a map under those 3 string constants.
     * If there are no conflicts of styles of certain type, the corresponding list will be empty.
     * All changes should be applied to result styles.
     */
    public Map<String, List<Conflict<IStyle>>> mergeStyles() {
        Map<String, List<Conflict<IStyle>>> styleConflictsMap = new HashMap<String, List<Conflict<IStyle>>>();

        for (String styleGroup : Arrays.asList(
                IStyleSheet.NORMAL_STYLES,
                IStyleSheet.AUTOMATIC_STYLES,
                IStyleSheet.MASTER_STYLES)) {

            List<Conflict<IStyle>> styleTriples = MergeUtils.findCorrespondingElements(
                    new ArrayList<IStyle>(source.getStyleSheet().getStyles(styleGroup)),
                    new ArrayList<IStyle>(target.getStyleSheet().getStyles(styleGroup)),
                    new ArrayList<IStyle>(result.getStyleSheet().getStyles(styleGroup))
            );
            List<Conflict<IStyle>> styleConflicts = new LinkedList<Conflict<IStyle>>();
            for (Conflict<IStyle> t : styleTriples) {
                if (t.getSource() != null) {
                    if (t.getTarget() != null) {
                        if (!WorkbookElementsComparator.areIdentical(t.getSource(), t.getTarget()))
                            styleConflicts.add(t);
                    } else {
                        result.getStyleSheet().addStyle(result.getStyleSheet().importStyle(t.getSource()), styleGroup);
                    }
                }
            }
            styleConflictsMap.put(styleGroup, styleConflicts);
        }

        return styleConflictsMap;
    }


}
