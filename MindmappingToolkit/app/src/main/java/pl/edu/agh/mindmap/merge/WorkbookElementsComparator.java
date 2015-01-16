package pl.edu.agh.mindmap.merge;

import org.xmind.core.IRelationship;
import org.xmind.core.IWorkbook;
import org.xmind.core.IWorkbookComponent;
import org.xmind.core.style.IStyle;
import org.xmind.core.style.IStyled;
import org.xmind.core.util.Property;
import pl.edu.agh.mindmap.Utils;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WorkbookElementsComparator {

    public static boolean areIdentical(IRelationship r1, IRelationship r2) {
        if (r1 == null || r2 == null)
            return false;
        if (r1.getModifiedTime() == r2.getModifiedTime())
            return true;
        if (!Utils.safeEquals(r1.getEnd1Id(), r2.getEnd1Id())
                || !Utils.safeEquals(r1.getEnd2Id(),r2.getEnd2Id())
                || !Utils.safeEquals(r1.getStyleId(),r2.getStyleId()))
            return false;
        String title1 = r1.getTitleText();
        String title2 = r2.getTitleText();
        if (title1 != null) {
            return title2 != null && title2.equals(title1);
        } else return title2 == null;
    }

    public static boolean identicalStyles(IStyled e1, IStyled e2){
        if(!(e1 instanceof IWorkbookComponent) || !(e2 instanceof IWorkbookComponent))
            throw new IllegalArgumentException("Elements are not workbook components");
        IWorkbookComponent c1 = (IWorkbookComponent)e1;
        IWorkbookComponent c2 = (IWorkbookComponent)e2;
        return areIdentical(c1.getOwnedWorkbook().getStyleSheet().findStyle(e1.getStyleId()),c2.getOwnedWorkbook().getStyleSheet().findStyle(e2.getStyleId()));
    }

    public static boolean areIdentical(IStyle s1, IStyle s2) {
        if(s1==null && s2==null)
            return true;
        return !(s1 == null || s2 == null)
                && !(!s1.getId().equals(s2.getId())
                    || !Utils.safeEquals(s1.getType(), s2.getType())
                    || !Utils.safeEquals(s1.getName(), s2.getName()))
                && equalProperties(s1.defaultStyles(), s2.defaultStyles())
                && equalProperties(s1.properties(), s2.properties());
    }

    private static boolean equalProperties(Iterator<Property> it1, Iterator<Property> it2) {
        Map<String, String> props1 = new HashMap<String, String>();
        while(it1.hasNext()) {
            Property p = it1.next();
            props1.put(p.key, p.value);
        }

        int i=0;
        while(it2.hasNext()){
            Property p = it2.next();
            if(!Utils.safeEquals(p.value,props1.get(p.key)))
                return false;
            i++;
        }
        return i == props1.size();
    }
}
