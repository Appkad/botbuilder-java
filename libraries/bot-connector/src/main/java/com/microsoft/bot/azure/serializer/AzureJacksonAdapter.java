/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.bot.azure.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.bot.rest.protocol.SerializerAdapter;
import com.microsoft.bot.rest.serializer.JacksonAdapter;

/**
 * A serialization helper class overriding {@link JacksonAdapter} with extra
 * functionality useful for Azure operations.
 */
public final class AzureJacksonAdapter extends JacksonAdapter implements SerializerAdapter<ObjectMapper> {
    /**
     * Creates an instance of the Azure flavored Jackson adapter.
     */
    public AzureJacksonAdapter() {
        super();
        serializer().registerModule(CloudErrorDeserializer.getModule(simpleMapper()));
    }
}
