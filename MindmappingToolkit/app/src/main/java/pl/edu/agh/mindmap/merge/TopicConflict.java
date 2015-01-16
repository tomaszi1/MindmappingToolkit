package pl.edu.agh.mindmap.merge;

import org.xmind.core.ITopic;
import pl.edu.agh.mindmap.Utils;

public class TopicConflict extends Conflict<ITopic> {
    private boolean identicalPosition;
    private boolean identicalHyperlink;
    private boolean identicalStructureClass;
    private boolean identicalTitleWidth;
    private boolean identicalTitleText;
    private boolean identicalType;
    private boolean identicalImage;
    private boolean identicalLabels;
    private boolean identicalMarkerRefs;
    private boolean identicalNotes;
    private boolean identicalNumbering;
    private boolean identicalStyle;
    private boolean identicalAttachedProperty;

    public TopicConflict(ITopic source, ITopic target, ITopic result) {
        super(source, target, result);
        checkIdenticalProperties();
    }

    private void checkIdenticalProperties() {
        ITopic s = getSource();
        ITopic t = getTarget();
        // plain properties
        identicalHyperlink = Utils.safeEquals(s.getHyperlink(), t.getHyperlink());
        identicalStructureClass = Utils.safeEquals(s.getStructureClass(), t.getStructureClass());
        identicalTitleText = Utils.safeEquals(s.getTitleText(), t.getTitleText());
        identicalTitleWidth = s.getTitleWidth() == t.getTitleWidth();
        identicalType = Utils.safeEquals(s.getType(), t.getType());
        identicalAttachedProperty = s.isAttached() == t.isAttached();

        // complex properties
        identicalPosition = TopicComparator.identicalPositions(s.getPosition(), t.getPosition());
        identicalImage = TopicComparator.identicalImage(s.getImage(), t.getImage());
        identicalLabels = TopicComparator.identicalLabels(s.getLabels(), t.getLabels());
        identicalMarkerRefs = TopicComparator.identicalMarkerRefs(s.getMarkerRefs(), t.getMarkerRefs());
        identicalNotes = TopicComparator.identicalNotes(s.getNotes(), t.getNotes());
        identicalNumbering = TopicComparator.identicalNumbering(s.getNumbering(), t.getNumbering());
        identicalStyle = WorkbookElementsComparator.identicalStyles(s, t);
    }


    public boolean areEntirelyIdentical() {
        return identicalHyperlink
                && identicalPosition
                && identicalStructureClass
                && identicalTitleText
                && identicalTitleWidth
                && identicalType
                && identicalImage
                && identicalLabels
                && identicalMarkerRefs
                && identicalNotes
                && identicalNumbering
                && identicalStyle
                && identicalAttachedProperty;
    }

    public boolean haveIdenticalPosition() {
        return identicalPosition;
    }

    public boolean haveIdenticalHyperlink() {
        return identicalHyperlink;
    }

    public boolean haveIdenticalStructureClass() {
        return identicalStructureClass;
    }

    public boolean haveIdenticalTitleWidth() {
        return identicalTitleWidth;
    }

    public boolean haveIdenticalTitleText() {
        return identicalTitleText;
    }

    public boolean haveIdenticalType() {
        return identicalType;
    }

    public boolean haveIdenticalImage() {
        return identicalImage;
    }

    public boolean haveIdenticalLabels() {
        return identicalLabels;
    }

    public boolean haveIdenticalMarkerRefs() {
        return identicalMarkerRefs;
    }

    public boolean haveIdenticalNotes() {
        return identicalNotes;
    }

    public boolean haveIdenticalNumbering() {
        return identicalNumbering;
    }

    public boolean haveIdenticalStyle() {
        return identicalStyle;
    }

    public boolean haveSameAttachment() {
        return identicalAttachedProperty;
    }
}
