package com.ledger.presentation.accountheading.delete;

import com.ledger.application.accountheading.DeleteAccountHeadingRequest;
import com.ledger.application.accountheading.DeleteAccountHeadingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeleteAccountHeadingMapper {

    @Mapping(source = "id", target = "headingId")
    DeleteAccountHeadingRequest toRequest(
            DeleteAccountHeadingInput input
    );

    DeleteAccountHeadingOutput toOutput(
            DeleteAccountHeadingResponse response
    );
}