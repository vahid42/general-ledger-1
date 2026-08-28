package com.ledger.presentation.accountheading.search;

import com.ledger.application.accountheading.SearchAccountHeadingsRequest;
import com.ledger.application.accountheading.SearchAccountHeadingsResponse;
import com.ledger.domain.accountheading.AccountNature;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SearchAccountHeadingsMapper {

    @Mapping(source = "nature", target = "nature")
    SearchAccountHeadingsRequest toRequest(
            SearchAccountHeadingsInput input
    );

    SearchAccountHeadingsOutput toOutput(
            SearchAccountHeadingsResponse.AccountHeadingItem item
    );

    List<SearchAccountHeadingsOutput> toOutput(
            List<SearchAccountHeadingsResponse.AccountHeadingItem> items
    );

    default AccountNature mapNature(String nature) {

        if (nature == null || nature.isBlank()) {
            return null;
        }

        return AccountNature.valueOf(
                nature.trim().toUpperCase()
        );
    }
}