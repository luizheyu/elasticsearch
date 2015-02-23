/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.alerts.scheduler.schedule;

import org.elasticsearch.alerts.AlertsSettingsException;
import org.elasticsearch.alerts.scheduler.schedule.support.MonthTimes;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class MonthlySchedule extends CronnableSchedule {

    public static final String TYPE = "monthly";

    public static final MonthTimes[] DEFAULT_TIMES = new MonthTimes[] { new MonthTimes() };

    private final MonthTimes[] times;

    MonthlySchedule() {
        this(DEFAULT_TIMES);
    }

    MonthlySchedule(MonthTimes... times) {
        super(crons(times));
        this.times = times;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public MonthTimes[] times() {
        return times;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (params.paramAsBoolean("normalize", false) && times.length == 1) {
            return  builder.value(times[0]);
        }
        return builder.value(times);
    }

    public static Builder builder() {
        return new Builder();
    }

    static String[] crons(MonthTimes[] times) {
        assert times.length > 0 : "at least one time must be defined";
        Set<String> crons = new HashSet<>(times.length);
        for (MonthTimes time : times) {
            crons.addAll(time.crons());
        }
        return crons.toArray(new String[crons.size()]);
    }

    public static class Parser implements Schedule.Parser<MonthlySchedule> {

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public MonthlySchedule parse(XContentParser parser) throws IOException {
            if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
                try {
                    return new MonthlySchedule(MonthTimes.parse(parser, parser.currentToken()));
                } catch (MonthTimes.ParseException pe) {
                    throw new AlertsSettingsException("could not parse [monthly] schedule. invalid month times", pe);
                }
            }
            if (parser.currentToken() == XContentParser.Token.START_ARRAY) {
                List<MonthTimes> times = new ArrayList<>();
                XContentParser.Token token;
                while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                    try {
                        times.add(MonthTimes.parse(parser, token));
                    } catch (MonthTimes.ParseException pe) {
                        throw new AlertsSettingsException("could not parse [monthly] schedule. invalid month times", pe);
                    }
                }
                return times.isEmpty() ? new MonthlySchedule() : new MonthlySchedule(times.toArray(new MonthTimes[times.size()]));
            }
            throw new AlertsSettingsException("could not parse [monthly] schedule. expected either an object or an array " +
                    "of objects representing month times, but found [" + parser.currentToken() + "] instead");
        }
    }

    public static class Builder {

        private final Set<MonthTimes> times = new HashSet<>();

        private Builder() {
        }

        public Builder time(MonthTimes time) {
            times.add(time);
            return this;
        }

        public Builder time(MonthTimes.Builder builder) {
            return time(builder.build());
        }

        public MonthlySchedule build() {
            return times.isEmpty() ? new MonthlySchedule() : new MonthlySchedule(times.toArray(new MonthTimes[times.size()]));
        }
    }

}
