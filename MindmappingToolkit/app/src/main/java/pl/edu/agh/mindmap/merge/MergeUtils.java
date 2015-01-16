package pl.edu.agh.mindmap.merge;

import org.xmind.core.IIdentifiable;
import org.xmind.core.IRevision;
import org.xmind.core.ISheet;

import java.util.*;

public class MergeUtils {

    public static <T extends IIdentifiable> List<Conflict<T>> findCorrespondingElements(List<T> source, List<T> target, List<T> result) {
        Map<String, Conflict<T>> triplesMap = new HashMap<String, Conflict<T>>();
        for (T i : source) {
            triplesMap.put(i.getId(), new Conflict<T>().setSource(i));
        }
        for (T i : target) {
            Conflict<T> conflict = triplesMap.get(i.getId());
            if (conflict == null)
                triplesMap.put(i.getId(), new Conflict<T>().setTarget(i));
            else
                conflict.setTarget(i);
        }
        if (result != null)
            for (T i : result) {
                Conflict<T> conflict = triplesMap.get(i.getId());
                if (conflict == null)
                    triplesMap.put(i.getId(), new Conflict<T>().setResult(i));
                else
                    conflict.setResult(i);
            }
        return new ArrayList<Conflict<T>>(triplesMap.values());
    }

    public static List<Conflict<ISheet>> findRevisionPairs(List<IRevision> sourceRevs, List<IRevision> targetRevs) {
        Map<Long, Conflict<ISheet>> revPairsMap = new HashMap<Long, Conflict<ISheet>>();
        for (IRevision i : sourceRevs) {
            ISheet s = (ISheet)i.getContent();
            revPairsMap.put(s.getModifiedTime(), new Conflict<ISheet>().setSource(s));
        }
        for (IRevision i : targetRevs) {
            ISheet s = (ISheet)i.getContent();
            Conflict<ISheet> conflict = revPairsMap.get(s.getModifiedTime());
            if (conflict == null)
                revPairsMap.put(s.getModifiedTime(), new Conflict<ISheet>().setTarget(s));
            else
                conflict.setTarget(s);
        }
        return new ArrayList<Conflict<ISheet>>(revPairsMap.values());
    }
}
