package pl.edu.agh.mindmap.merge;


import org.xmind.core.*;
import pl.edu.agh.mindmap.Utils;

import java.util.*;

/**
 * Class for merging sheets of the same ID.
 */
public class SheetMerger extends Conflict<ISheet> {
    private List<TopicConflict> topicConflicts = new ArrayList<TopicConflict>();
    private List<Conflict<IRelationship>> relationshipConflicts;
    private List<Conflict<ISummary>> summaryConflicts = new ArrayList<Conflict<ISummary>>();
    private List<Conflict<IBoundary>> boundaryConflicts = new ArrayList<Conflict<IBoundary>>();
    private List<ISummary> uncopiableSummaries = new ArrayList<ISummary>();
    private List<IBoundary> uncopiableBoundaries = new ArrayList<IBoundary>();

    SheetMerger(Conflict<ISheet> sheetsConflict) {
        super(sheetsConflict.getSource(), sheetsConflict.getTarget(), sheetsConflict.getResult());

        iterateSourceSheet();
        findRelationshipConflicts();
    }

    /**
     * Returns topic conflicts.
     */
    public List<Conflict<ITopic>> getTopicConflicts() {
        return new ArrayList<Conflict<ITopic>>(topicConflicts);
    }

    /**
     * Returns relationship conflicts.
     * Relationship conflict consists of source relationship, target relationship and result relationship.
     * All changes should be applied to result relationship!
     */
    public List<Conflict<IRelationship>> getRelationshipConflicts() {
        return new ArrayList<Conflict<IRelationship>>(relationshipConflicts);
    }

    public List<Conflict<ISummary>> getSummaryConflicts() {
        return summaryConflicts;
    }

    private void iterateSourceSheet() {
        ITopic rootTopic = getSource().getRootTopic();

        ITopic targetRoot = getTarget().getRootTopic();
        if (!targetRoot.getId().equals(rootTopic.getId()))
            throw new IllegalStateException("Root topic was replaced. Cannot merge sheets.");

        scanTopic(rootTopic);
    }

    private void scanTopic(ITopic sourceTopic) {
        List<ITopic> children = sourceTopic.getAllChildren();

        for (ITopic sourceChild : children) {
            if (Utils.safeEquals(sourceChild.getType(), ITopic.SUMMARY))
                continue;
            ITopic targetChild = getTarget().getOwnedWorkbook().findTopic(sourceChild.getId(), getTarget());
            if (targetChild != null) {
                if (targetChild.getModifiedTime() == sourceChild.getModifiedTime())
                    continue;
                ITopic resultChild = getResult().getOwnedWorkbook().findTopic(sourceChild.getId(), getResult());
                TopicConflict conflict = new TopicConflict(sourceChild, targetChild, resultChild);
                if(conflict.areEntirelyIdentical())
                    continue;
                topicConflicts.add(conflict);
            } else {
                importChild(sourceChild);
            }
            scanTopic(sourceChild);
        }

        scanSummaries(sourceTopic);
        scanBoundaries(sourceTopic);
    }

    private void scanSummaries(ITopic topic) {
        Iterator<ISummary> it = topic.getSummaries().iterator();
        while (it.hasNext()) {
            ISummary sourceSum = it.next();
            ISummary targetSum = (ISummary) getTarget().getOwnedWorkbook().findElement(sourceSum.getId(), getTarget());
            if (targetSum != null) {
                ISummary resultSum = (ISummary) getResult().getOwnedWorkbook().findElement(sourceSum.getId(), getResult());
                summaryConflicts.add(new Conflict<ISummary>(sourceSum, targetSum, resultSum));
            } else {
                tryImportSummary(sourceSum);
            }
        }
    }

    private void tryImportSummary(ISummary sourceSummary) {
        List<ITopic> enclosingTopics = sourceSummary.getEnclosingTopics();
        if (enclosingTopics.isEmpty())
            return;
        List<Integer> indexes = new ArrayList<Integer>();
        ITopic resultParent = getResult().getOwnedWorkbook().findTopic(enclosingTopics.get(0).getId(), getResult()).getParent();
        for (ITopic encTopic : enclosingTopics) {
            ITopic resultTopic = getResult().getOwnedWorkbook().findTopic(encTopic.getId(), getResult());
            if (!Utils.safeEquals(resultParent, resultTopic.getParent())) {
                uncopiableSummaries.add(sourceSummary);
                return;
            }
            indexes.add(resultTopic.getIndex());
        }

        Collections.sort(indexes);
        Iterator<Integer> it = indexes.iterator();
        int reqIndex = it.next() + 1;
        while (it.hasNext()) {
            int next = it.next();
            if (next != reqIndex) {
                uncopiableSummaries.add(sourceSummary);
                return;
            }
            reqIndex++;
        }

        ISummary impSummary = (ISummary) getResult().getOwnedWorkbook().importElement(sourceSummary);
        impSummary.setStartIndex(indexes.get(0));
        impSummary.setEndIndex(indexes.get(indexes.size() - 1));
        resultParent.addSummary(impSummary);

        ITopic impSumTopic = (ITopic) getResult().getOwnedWorkbook().importElement(sourceSummary.getTopic());
        resultParent.add(impSumTopic, ITopic.SUMMARY);
        for (ITopic child : impSumTopic.getAllChildren()) {
            impSumTopic.remove(child);
        }
        scanTopic(sourceSummary.getTopic());
    }

    private void scanBoundaries(ITopic topic) {
        Iterator<IBoundary> it = topic.getBoundaries().iterator();
        while (it.hasNext()) {
            IBoundary sourceBnd = it.next();
            IBoundary targetBnd = (IBoundary) getTarget().getOwnedWorkbook().findElement(sourceBnd.getId(), getTarget());
            if (targetBnd != null) {
                IBoundary resultBnd = (IBoundary) getResult().getOwnedWorkbook().findElement(sourceBnd.getId(), getResult());
                boundaryConflicts.add(new Conflict<IBoundary>(sourceBnd, targetBnd, resultBnd));
            } else {
                tryImportBoundary(sourceBnd);
            }
        }
    }

    private void tryImportBoundary(IBoundary sourceBoundary) {
        List<ITopic> enclosingTopics = sourceBoundary.getEnclosingTopics();
        if (enclosingTopics.isEmpty())
            return;
        List<Integer> indexes = new ArrayList<Integer>();
        ITopic resultParent = getResult().getOwnedWorkbook().findTopic(enclosingTopics.get(0).getId(), getResult()).getParent();
        for (ITopic encTopic : enclosingTopics) {
            ITopic resultTopic = getResult().getOwnedWorkbook().findTopic(encTopic.getId(), getResult());
            if (!Utils.safeEquals(resultParent, resultTopic.getParent())) {
                uncopiableBoundaries.add(sourceBoundary);
                return;
            }
            indexes.add(resultTopic.getIndex());
        }

        Collections.sort(indexes);
        Iterator<Integer> it = indexes.iterator();
        int reqIndex = it.next() + 1;
        while (it.hasNext()) {
            int next = it.next();
            if (next != reqIndex) {
                uncopiableBoundaries.add(sourceBoundary);
                return;
            }
            reqIndex++;
        }

        IBoundary impBoundary = (IBoundary) getResult().getOwnedWorkbook().importElement(sourceBoundary);
        impBoundary.setStartIndex(indexes.get(0));
        impBoundary.setEndIndex(indexes.get(indexes.size() - 1));
        resultParent.addBoundary(impBoundary);
    }

    private void importChild(ITopic sourceChild) {
        ITopic resultParent = getResult().getOwnedWorkbook().findTopic(sourceChild.getParent().getId(), getResult());
        ITopic impTopic = (ITopic) getResult().getOwnedWorkbook().importElement(sourceChild);
        resultParent.add(impTopic);
        for (ITopic child : impTopic.getAllChildren()) {
            impTopic.remove(child);
        }
    }

    private void findRelationshipConflicts() {
        relationshipConflicts = MergeUtils.findCorrespondingElements(
                new ArrayList<IRelationship>(getSource().getRelationships()),
                new ArrayList<IRelationship>(getTarget().getRelationships()),
                new ArrayList<IRelationship>(getResult().getRelationships())
        );
        Iterator<Conflict<IRelationship>> it = relationshipConflicts.iterator();
        while (it.hasNext()) {
            Conflict<IRelationship> t = it.next();
            if (t.getSource() == null || t.getTarget() == null) {
                if (t.getSource() != null) {
                    getResult().addRelationship((IRelationship) getResult().getOwnedWorkbook().importElement(t.getSource()));
                }
                it.remove();
            }
        }
    }

    public List<ISummary> getUncopiableSummaries() {
        return new ArrayList<ISummary>(uncopiableSummaries);
    }

    public List<Conflict<IBoundary>> getBoundaryConflicts() {
        return new ArrayList<Conflict<IBoundary>>(boundaryConflicts);
    }

    public List<IBoundary> getUncopiableBoundaries() {
        return new ArrayList<IBoundary>(uncopiableBoundaries);
    }
}
