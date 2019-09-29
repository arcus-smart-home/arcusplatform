package com.iris.platform.address.validation;

import com.iris.messages.model.Place;
import com.iris.platform.address.StreetAddress;

import java.util.LinkedList;
import java.util.List;

public class NoopAddressValidator implements AddressValidator {
    @Override
    public AddressValidationResult validate(StreetAddress address) {
        List<StreetAddress> addresses = new LinkedList<StreetAddress>();
        addresses.add(address);
        return new AddressValidationResult(addresses, true);
    }
}
