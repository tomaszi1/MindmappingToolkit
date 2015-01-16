package pl.edu.agh.mindmap.gdrive;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

import java.util.ArrayList;
import java.util.List;

public class GDriveFile {
    private String id;
    private String name;
    private String parentId;
    private boolean isFolder;
    private List<GDriveFile> content;
    private String downloadUrl;
    private String mimeType;
    private String revision;

    GDriveFile(File file, String parentId, List<GDriveFile> content) {
        name = file.getTitle();
        id = file.getId();
        if (parentId == null) {
            List<ParentReference> parents = file.getParents();
            if (!parents.isEmpty())
                parents.get(0).getId();
        } else {
            this.parentId = parentId;
        }
        isFolder = Utils.isFolder(file);
        this.content = content;
        if (!isFolder)
            downloadUrl = file.getDownloadUrl();
        this.mimeType = file.getMimeType();
        this.revision = file.getHeadRevisionId();
    }

    GDriveFile(String id, boolean isFolder) {
        this.id = id;
        this.isFolder = isFolder;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getParentId() {
        return parentId;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public List<GDriveFile> getContent() {
        return new ArrayList<GDriveFile>(content);
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getRevision() {
        return revision;
    }
}
