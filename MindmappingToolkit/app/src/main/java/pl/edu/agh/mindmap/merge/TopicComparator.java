package pl.edu.agh.mindmap.merge;

import org.xmind.core.*;
import org.xmind.core.marker.IMarkerRef;
import org.xmind.core.util.Point;
import pl.edu.agh.mindmap.Utils;

import java.util.*;

public class TopicComparator {

    public static boolean identicalPositions(Point a, Point b) {
        if (a == null)
            return b == null;
        return b != null && a.x == b.x && a.y == b.y;
    }

    public static boolean identicalImage(IImage i1, IImage i2) {
        return Utils.safeEquals(i1.getSource(), i2.getSource())
                && Utils.safeEquals(i1.getWidth(), i2.getWidth())
                && Utils.safeEquals(i1.getHeight(), i2.getHeight())
                && Utils.safeEquals(i1.getAlignment(), i2.getAlignment());
    }

    public static boolean identicalLabels(Set<String> l1, Set<String> l2) {
        return l1.containsAll(l2) && l2.containsAll(l1);
    }

    public static boolean identicalMarkerRefs(Set<IMarkerRef> mr1, Set<IMarkerRef> mr2) {
        Iterator<IMarkerRef> it = mr1.iterator();
        Map<String, Pair<IMarkerRef>> map = new HashMap<String, Pair<IMarkerRef>>();
        while (it.hasNext()) {
            IMarkerRef next = it.next();
            map.put(next.getMarkerId(), new Pair<IMarkerRef>(next, null));
        }
        it = mr2.iterator();
        while (it.hasNext()) {
            IMarkerRef next = it.next();
            Pair<IMarkerRef> pair = map.get(next.getMarkerId());
            if (pair == null)
                return false;
            pair.setRight(next);
        }
        for (Pair<IMarkerRef> pair : map.values()) {
            if (!pair.isFull() || !Utils.safeEquals(pair.getLeft().getDescription(), pair.getRight().getDescription()))
                return false;
        }
        return true;
    }

    public static boolean identicalNotes(INotes n1, INotes n2) {
        if (n1.isEmpty())
            return n2.isEmpty();
        if (n2.isEmpty())
            return false;
        // compare plain notes
        IPlainNotesContent plain1 = (IPlainNotesContent) n1.getContent(INotes.PLAIN);
        IPlainNotesContent plain2 = (IPlainNotesContent) n2.getContent(INotes.PLAIN);
        if (!Utils.safeEquals(plain1.getTextContent(), plain2.getTextContent()))
            return false;
        // compare HTML notes
        IHtmlNotesContent h1 = (IHtmlNotesContent) n1.getContent(INotes.HTML);
        IHtmlNotesContent h2 = (IHtmlNotesContent) n2.getContent(INotes.HTML);
        Iterator<IParagraph> it1 = h1.getParagraphs().iterator();
        Iterator<IParagraph> it2 = h2.getParagraphs().iterator();
        while (it1.hasNext()) {
            if (!it2.hasNext())
                return false;
            IParagraph p1 = it1.next();
            IParagraph p2 = it2.next();
            if (!WorkbookElementsComparator.identicalStyles(p1, p2)
                    || !identicalSpanLists(p1.getSpans(), p2.getSpans()))
                return false;
        }
        return true;
    }

    private static boolean identicalSpanLists(List<ISpan> l1, List<ISpan> l2) {
        Iterator<ISpan> it1 = l1.iterator();
        Iterator<ISpan> it2 = l2.iterator();
        while (it1.hasNext()) {
            if (!it2.hasNext())
                return false;
            ISpan s1 = it1.next();
            ISpan s2 = it2.next();
            if (!WorkbookElementsComparator.identicalStyles(s1, s2))
                return false;
            if (s1 instanceof ITextSpan) {
                if(!(s2 instanceof ITextSpan))
                    return false;
                ITextSpan ts1 = (ITextSpan) s1;
                ITextSpan ts2 = (ITextSpan) s2;
                if (!Utils.safeEquals(ts1.getTextContent(), ts2.getTextContent()))
                    return false;
            } else if (s1 instanceof IHyperlinkSpan) {
                if(!(s2 instanceof IHyperlinkSpan))
                    return false;
                IHyperlinkSpan hs1 = (IHyperlinkSpan) s1;
                IHyperlinkSpan hs2 = (IHyperlinkSpan) s2;
                if (!Utils.safeEquals(hs1.getHref(), hs2.getHref())
                        || !identicalSpanLists(hs1.getSpans(), hs2.getSpans()))
                    return false;
            } else if (s1 instanceof IImageSpan) {
                if(!(s2 instanceof IImageSpan))
                    return false;
                IImageSpan is1 = (IImageSpan) s1;
                IImageSpan is2 = (IImageSpan) s2;
                if (!Utils.safeEquals(is1.getSource(), is2.getSource()))
                    return false;
            }
        }
        return true;
    }

    public static boolean identicalNumbering(INumbering n1, INumbering n2) {
        return Utils.safeEquals(n1.getNumberFormat(),n2.getNumberFormat())
                && Utils.safeEquals(n1.getPrefix(),n2.getPrefix())
                && Utils.safeEquals(n1.getSuffix(),n2.getSuffix());
    }
}
