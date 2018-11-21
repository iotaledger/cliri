package com.iota.iri.service.tipselection.impl;

import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.EntryPointSelector;
import org.junit.Assert;
import org.junit.Test;

public class EntryPointSelectorGenesisImplTest {

    @Test
    public void testEntryPointBWithTangleData() throws Exception {

        EntryPointSelector entryPointSelector = new EntryPointSelectorGenesisImpl();
        Hash entryPoint = entryPointSelector.getEntryPoint(10);

        Assert.assertEquals("The entry point should be the milestone in the Tangle", Hash.NULL_HASH, entryPoint);
    }
}
