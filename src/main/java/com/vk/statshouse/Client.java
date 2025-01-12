// Copyright 2023 V Kontakte LLC
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.vk.statshouse;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class Client implements Closeable {

    public static final int DEFAULT_PORT = 13337;
    public static final String TAG_STRING_TOP = "_s";
    public static final String TAG_HOST = "_h";
    private static final String[] defaultTags = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
    private static final String[] defaultTagsValues = new String[]{};
    private static final String envName = "env";
    private static final String envNum = "0";

    final Transport transport;

    public Client(InetAddress shHost, int shPort, String env) throws SocketException {
        transport = new Transport(shHost, shPort, env);
    }

    Client(String env) {
        transport = new Transport(env);
    }

    public MetricRef getMetric(String name) {
        return new MetricRefImpl(name);
    }

    public void flush() throws IOException {
        transport.flush();
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }


    class MetricRefImpl implements MetricRef {
        final String name;
        final String[] tagsNames;
        final String[] tagsValues;
        final long unixTime;
        final boolean hasEnv;

        private MetricRefImpl(String name) {
            this.name = name;
            this.tagsValues = defaultTagsValues;
            this.tagsNames = defaultTags;
            this.unixTime = 0;
            this.hasEnv = false;
        }

        private MetricRefImpl(String name, boolean hasEnv, String[] tagsNames, String[] tagsValues, long unixTime, String newTagName, String newTagValue) {
            this.name = name;
            if (!"".equals(newTagName)) {
                this.tagsNames = Arrays.copyOf(tagsNames, Math.max(defaultTags.length, tagsValues.length + 1));
                this.tagsNames[tagsValues.length] = newTagName;
            } else {
                this.tagsNames = tagsNames;
            }
            this.tagsValues = Arrays.copyOf(tagsValues, tagsValues.length + 1);
            this.tagsValues[tagsValues.length] = newTagValue;
            this.unixTime = unixTime;
            this.hasEnv = hasEnv || envName.equals(newTagName) || envNum.equals(newTagName);
        }

        private MetricRefImpl(String name, boolean hasEnv, String[] tagsNames, String[] tagsValues, int tagsLength, long unixTime, String... newTags) {
            this.name = name;
            this.tagsNames = tagsNames;
            this.tagsValues = Arrays.copyOf(tagsValues, tagsLength + newTags.length);
            System.arraycopy(newTags, 0, this.tagsValues, tagsLength, newTags.length);
            this.tagsLength = this.tagsValues.length;
            this.unixTime = unixTime;
            this.hasEnv = hasEnv;

            assert tagsValues.length <= tagsNames.length;
        }


        private MetricRefImpl(String name, boolean hasEnv, String[] tagsNames, String[] tagsValues, long unixTime) {
            this.name = name;
            this.tagsNames = tagsNames;
            this.tagsValues = tagsValues;
            this.unixTime = unixTime;
            this.hasEnv = hasEnv;
        }

        public MetricRef tag(String v) {
            return new MetricRefImpl(name, hasEnv, tagsNames, tagsValues, unixTime, "", v);
        }

        public MetricRef tags(String... v) {
            return new MetricRefImpl(name, hasEnv, tagsNames, tagsValues, tagsLength, unixTime, v);
        }

        @Override
        public MetricRef tag(String name, String v) {
            return new MetricRefImpl(this.name, hasEnv, tagsNames, tagsValues, unixTime, name, v);
        }

        public MetricRef time(long unixTime) {
            return new MetricRefImpl(name, hasEnv, tagsNames, tagsValues, unixTime);
        }

        public void count(double count) throws IOException {
            Client.this.transport.writeCount(hasEnv, name, tagsNames, tagsValues, count, unixTime);
        }

        public void value(double value) throws IOException {
            Client.this.transport.writeValue(hasEnv, name, tagsNames, tagsValues, new double[]{value}, unixTime);
        }

        public void values(double[] values) throws IOException {
            Client.this.transport.writeValue(hasEnv, name, tagsNames, tagsValues, values, unixTime);
        }

        public void unique(long value) throws IOException {
            Client.this.transport.writeUnique(hasEnv, name, tagsNames, tagsValues, new long[]{value}, unixTime);
        }

        public void uniques(long[] value) throws IOException {
            Client.this.transport.writeUnique(hasEnv, name, tagsNames, tagsValues, value, unixTime);
        }
    }
}
