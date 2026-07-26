package com.learningplatform.content.domain;

public enum ContentType {
    GENERAL,
    // Legacy values remain readable until database/003_unify_content_type.sql is executed.
    ARTICLE,
    DOCUMENT,
    VIDEO,
    ATTACHMENT,
    MIXED
}
