package io.github.lstramke.coincollector.model.DTOs.Requests;

import java.util.List;

import io.github.lstramke.coincollector.model.EuroCoin;

public record CreateCollectionRequest(String name, String groupId, List<EuroCoin> coins) {}
