package com.ledger.presentation.accountheading.create;

import com.ledger.application.accountheading.CreateAccountHeadingRequest;
import com.ledger.application.accountheading.CreateAccountHeadingResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CreateAccountHeadingMapper {

     CreateAccountHeadingRequest toRequest(
            CreateAccountHeadingInput input
    );

    CreateAccountHeadingOutput toOutput(
            CreateAccountHeadingResponse response
    );
}