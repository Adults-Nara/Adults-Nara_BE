package com.ott.core.modules.tag.dto.response;

import java.util.List;

public record ParentTagWithChildResponse(
        String tagId,
        String tagName,
        List<ChildTagResponse> childTagList
) {
}
