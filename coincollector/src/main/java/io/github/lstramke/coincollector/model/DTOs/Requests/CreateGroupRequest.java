package io.github.lstramke.coincollector.model.DTOs.Requests;

import java.util.List;

public record CreateGroupRequest(String name, List<String> collectionIds ) {
    
}
