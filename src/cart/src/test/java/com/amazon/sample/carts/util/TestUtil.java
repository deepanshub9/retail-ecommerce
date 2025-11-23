/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

package com.amazon.sample.carts.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class TestUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        return objectMapper.writeValueAsBytes(object);
    }

    public static String convertObjectToJsonString(Object object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }
}