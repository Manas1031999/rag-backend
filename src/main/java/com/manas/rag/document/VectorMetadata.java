package com.manas.rag.document;

import java.util.UUID;

/** Names of the metadata fields saved next to every embedding, and the filters built on them. */
public final class VectorMetadata {

    public static final String WORKSPACE_ID = "workspaceId";
    public static final String DOCUMENT_ID = "documentId";
    public static final String FILENAME = "filename";
    public static final String PAGE = "page";
    public static final String CHUNK_INDEX = "chunkIndex";

    private VectorMetadata() {
    }

    // The ids are UUIDs produced by the database, so they are safe to place inside the filter text.
    public static String workspaceFilter(UUID workspaceId) {
        return WORKSPACE_ID + " == '" + workspaceId + "'";
    }

    public static String documentFilter(UUID documentId) {
        return DOCUMENT_ID + " == '" + documentId + "'";
    }
}
