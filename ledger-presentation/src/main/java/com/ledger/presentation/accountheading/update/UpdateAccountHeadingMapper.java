package com.ledger.presentation.accountheading.update;

import com.ledger.application.accountheading.UpdateAccountHeadingRequest;
import com.ledger.application.accountheading.UpdateAccountHeadingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UpdateAccountHeadingMapper {

    @Mapping(source = "id", target = "headingId")
    UpdateAccountHeadingRequest toRequest(
            UpdateAccountHeadingInput input
    );

    UpdateAccountHeadingOutput toOutput(
            UpdateAccountHeadingResponse response
    );
}