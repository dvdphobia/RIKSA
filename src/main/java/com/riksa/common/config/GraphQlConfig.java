package com.riksa.common.config;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Configuration
public class GraphQlConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        GraphQLScalarType uuidScalar = GraphQLScalarType.newScalar()
                .name("UUID")
                .description("UUID scalar")
                .coercing(new Coercing<UUID, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof UUID uuid) {
                            return uuid.toString();
                        }
                        throw new IllegalArgumentException("Expected UUID");
                    }

                    @Override
                    public UUID parseValue(Object input) {
                        return UUID.fromString(input.toString());
                    }

                    @Override
                    public UUID parseLiteral(Object input) {
                        if (input instanceof StringValue sv) {
                            return UUID.fromString(sv.getValue());
                        }
                        throw new IllegalArgumentException("Expected StringValue");
                    }
                })
                .build();

        GraphQLScalarType dateTimeScalar = GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("ISO-8601 date-time scalar")
                .coercing(new Coercing<OffsetDateTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof OffsetDateTime dt) {
                            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dt);
                        }
                        throw new IllegalArgumentException("Expected OffsetDateTime");
                    }

                    @Override
                    public OffsetDateTime parseValue(Object input) {
                        return OffsetDateTime.parse(input.toString());
                    }

                    @Override
                    public OffsetDateTime parseLiteral(Object input) {
                        if (input instanceof StringValue sv) {
                            return OffsetDateTime.parse(sv.getValue());
                        }
                        throw new IllegalArgumentException("Expected StringValue");
                    }
                })
                .build();

        return wiringBuilder -> wiringBuilder
                .scalar(uuidScalar)
                .scalar(dateTimeScalar);
    }
}
